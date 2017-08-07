import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import Punter
import groovy.transform.CompileStatic

import java.util.concurrent.Executors
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

jsonSlurper = new JsonSlurper()

def claimRiver(claim) {
    rivers.each { r ->
        if ((r.source == claim.source && r.target == claim.target)
            || (r.source == claim.target && r.target == claim.source)) {
            if (r.owner < 0) {
                r.owner = claim.punter
            }
            else if (r.option >= 0 || r.owner == claim.punter) {
                println "Error: river already claimed by ${r.owner}"
                System.exit(1)
            }
            else {
                r.option = claim.punter
            }
        }
    }
}

def claimSplurge(splurge) {
    def claim = [punter: splurge.punter, source: 0, target: 0]
    int n = splurge.route.size()
    for (int i = 1; i < n; i++) {
        claim.source = splurge.route.get(i-1)
        claim.target = splurge.route.get(i)
        claimRiver(claim)
    }
}

def shorten(s) {
    // return s
    if (s.length() <= 70) {
        s
    } else {
        s[0..57] + "  #####  " + s[-5..-1]
    }
}

def readJson(InputStream inp) {
    int len = 0
    for (;;) {
        int ch = inp.read()
        if (ch == ':') {
            break
        }
        len = 10 * len + Character.getNumericValue(ch)
    }
    byte[] data = new byte[len]
    for (int n = 0; n < len; ) {
        int m = inp.read(data, n, len - n);
        if (m < 0) {
            throw new RuntimeException("unexpected end of input at " + n);
        }
        n += m;
    }
    println "RECV ${shorten(new String(data))}"
    return jsonSlurper.parse(data)
}

def writeJson(PrintStream out, obj) {
    String json = JsonOutput.toJson(obj)
    int n = json.length()
    out.print(n)
    out.write(':' as char)
    out.print(json)
    out.flush()
    println "SENT ${shorten(json)}"
}

executor = // new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new SynchronousQueue<>())
        Executors.newFixedThreadPool(1)

PIPE_SIZE = 16384

def startPunter(punter) {
    if (punter.external) {
        def process = new ProcessBuilder(punter.command).start()
        [inp: process.getIn(),
         out: new PrintStream(process.getOut()),
         state: process
        ]
    } else {
        PipedOutputStream out = new PipedOutputStream()
        PipedInputStream inp = new PipedInputStream(out, PIPE_SIZE)
        PipedInputStream in2 = new PipedInputStream(PIPE_SIZE)
        [ inp: inp,
          out: new PrintStream(new PipedOutputStream(in2)),
          task: executor.submit {Punter.runOffline(punter.command, in2, new PrintStream(out))}
        ]
    }
}

def stopPunter(punter, pstate) {
    if (punter.external) {
        pstate.state.waitFor()
    } else {
        pstate.task.get()
        pstate.inp.close()
        pstate.out.close()
    }
}


def runSetup(punter, id) {
    punter.id = id
    println "SETUP ${punter} ..."
    def pstate = startPunter(punter)
    def msg1 = readJson(pstate.inp)
    punter.name = msg1.me
    writeJson(pstate.out, [you: punter.name])
    def time = System.nanoTime()
    writeJson(pstate.out, [punter: punter.id, punters: numPunters, map: map, settings: [futures: true, splurges: true]])
    def msg2 = readJson(pstate.inp)
    punter.state = msg2.state
    time = (System.nanoTime() - time) / 1.0e9;
    println(String.format("TIME %.3f seconds", time))
    if (time > 10) {
        println "TIMEOUT in setup phase"
        System.exit(0)
    }
    stopPunter(punter, pstate)
    punter.lastMove = [pass: [punter: punter.id]]
    punter.setupTime = time
}

def getLastMoves() {
    punters.collect { p -> p.lastMove }
}

def runMove(punter) {
    println "### Getting move from punter ${punter.id} ..."
    def pstate = startPunter(punter)
    def msg1 = readJson(pstate.inp)
    punter.name = msg1.me
    writeJson(pstate.out, [you: punter.name])
    def time = System.nanoTime()
    writeJson(pstate.out, [move: [moves: getLastMoves()], state: punter.state])
    def msg2 = readJson(pstate.inp)
    time = (System.nanoTime() - time) / 1.0e9
    if (msg2.containsKey('claim')) {
        println "CLAIM ${msg2.claim}"
        claimRiver(msg2.claim)
    } else if (msg2.containsKey('splurge')) {
        println "SPLURGE ${msg2.splurge}"
        claimSplurge(msg2.splurge)
    } else if (msg2.containsKey('option')) {
        claimRiver(msg2.option)
    } else {
        println "PASS ${msg2.pass}"
    }
    println(String.format("TIME %.3f seconds", time))
    if (time > 1) {
        println "TIMEOUT in game phase"
        System.exit(0)
    }
    punter.moveTime += time
    punter.moves++
    if (time > punter.maxTime) {
        punter.maxTime = time
    }
    punter.state = msg2.state
    msg2.remove('state')
    punter.lastMove = msg2
    stopPunter(punter, pstate)
}

def computeScore(int punter) {
    int score = 0
    map.mines.each { mine ->
        map.sites.each { site ->
            if (site.id != mine && pathOnOwnRivers(punter, mine, site.id)) {
                int d = shortestPath(mine, site.id)
                score += d * d
            }
        }
    }
    score
}

def pathOnOwnRivers(int punter, int a, int b) {
    bfs(a, b, rivers.findAll { r -> r.owner == punter }) >= 0
}

def shortestPath(int a, int b) {
    bfs(a, b, rivers)
}

def enterTable(Map<Integer, List> table, int node, river) {
    List e = table.get(node)
    if (e == null) {
        e = new ArrayList()
        table.put(node, e)
    }
    e.add(river)
}

def bfs(int source, int target, rivers) {
    Map<Integer, List> riversByNode = new HashMap<>()
    rivers.each { river ->
        enterTable(riversByNode, river.source, river)
        enterTable(riversByNode, river.target, river)
    }

    Map<Integer,Integer> visited = new HashMap<>()
    Queue<Integer> queue = new LinkedList<>()
    visited.put(source, -1)
    queue.addLast(source)
    while (!queue.isEmpty()) {
        int node = queue.removeFirst()
        if (node == target) {
            int dist = 0
            for (;;) {
                int orig = visited.get(node)
                if (orig < 0) {
                    return dist
                }
                dist++
                node = orig
            }
        }
        List rvs = riversByNode.get(node)
        if (rvs != null) {
            rvs.each { r ->
                if (r.source == node && !visited.containsKey(r.target)) {
                    queue.addLast(r.target)
                    visited.put(r.target, node)
                } else if (!visited.containsKey(r.source)) {
                    queue.addLast(r.source)
                    visited.put(r.source, node)
                }
            }
        }
    }
    return -1
}


// ===================================

if (args.length < 3) {
    System.err.println "usage: server map punter1 punter2 ..."
    System.exit(1)
}
String mapFile = args[0]
punters = args[1..-1].collect { p ->
    [command: p, external: new File(p).canExecute(),
     moves: 0, moveTime: 0.0]
}
numPunters = punters.size()

println "Map: ${mapFile}"
println "Punters: ${numPunters}"

map = jsonSlurper.parse(new File(mapFile))
// read again to get a copy of the rivers
rivers = jsonSlurper.parse(new File(mapFile)).rivers

println "Map: ${map.sites.size()} sites, ${rivers.size()} rivers, ${map.mines.size()} mines."
println "Site id range: ${map.sites.collect({it.id}).min()}-${map.sites.collect({it.id}).max()}"

punters.eachWithIndex { p,id ->
    runSetup(p,id)
    println "Punter ${id} initialized: ${p.name}"
}

int nMoves = rivers.size()
for (int move = 0; move < nMoves; move++) {
    int punter = move % numPunters
    println "### Move ${move}/${nMoves}"
    runMove(punters[punter])
}
executor.shutdown()

// sending stop seems unnecessary

// println "Final claims: ${rivers}"
double totalTime = 0.0
punters.each { p ->
    int numRivers = rivers.count { it.owner == p.id }
    println(String.format("SCORE %2d: %7d (moves: %4d, setup: %4.0fms, move: %3.0fms avg, %3.0fms max, claimed: %4d)  %s",
                          p.id, computeScore(p.id), p.moves, 1000 * p.setupTime,
                          1000 * p.moveTime / p.moves, 1000 * p.maxTime,
                          numRivers, p.name))
    totalTime += p.moveTime
}
println(String.format("Total move time: %.3f seconds", totalTime))