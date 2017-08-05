import groovy.json.JsonOutput
import groovy.json.JsonSlurper

jsonSlurper = new JsonSlurper()

def countUnclaimed(rivers) {
    rivers.count { !it.containsKey('owner') }
}

def claimRiver(claim) {
    rivers.each { r ->
        if ((r.source == claim.source && r.target == claim.target)
            || (r.source == claim.target && r.target == claim.source)) {
            if (r.owner >= 0) {
                if (r.owner == claim.punter) {
                    println "Redundant claim!"
                }
                println "Error: river already claimed by ${r.owner}"
                System.exit(1)
            } else {
                r.owner = claim.punter
            }
        }
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
    // println "RECEIVED ${new String(data)}"
    return jsonSlurper.parse(data)
}

def writeJson(PrintStream out, obj) {
    String json = JsonOutput.toJson(obj)
    int n = json.length()
    out.print(n)
    out.write(':' as char)
    out.print(json)
    out.flush()
    // println "SENT ${json}"
}

def runSetup(punter, id) {
    punter.id = id
    println "Setting up punter ${punter}..."
    def process = new ProcessBuilder(punter.command)
                        .start()
    def inp = process.getIn()
    def msg1 = readJson(inp)
    punter.name = msg1.me
    def out = new PrintStream(process.getOut())
    writeJson(out, [you: punter.name])
    long time = System.nanoTime()
    writeJson(out, [punter: punter.id, punters: numPunters, map: map])
    def msg2 = readJson(inp)
    punter.state = msg2.state
    time = System.nanoTime() - time
    println(String.format("Time: %.1f seconds", time / 1.0e9))
    if (time > 1.0e10) {
        println "TIMEOUT in setup phase"
        System.exit(0)
    }
    process.waitFor()
    punter.lastMove = [pass: [punter: punter.id]]
}

def getLastMoves() {
    punters.collect { p -> p.lastMove }
}

def runMove(punter) {
    println "Getting move from punter ${punter.id}..."
    def process = new ProcessBuilder(punter.command)
            .start()
    def inp = process.getIn()
    def msg1 = readJson(inp)
    punter.name = msg1.me
    def out = new PrintStream(process.getOut())
    writeJson(out, [you: punter.name])
    long time = System.nanoTime()
    writeJson(out, [move: [moves: getLastMoves()], state: punter.state])
    def msg2 = readJson(inp)
    if (msg2.containsKey('claim')) {
        println "claim: ${msg2.claim}"
        claimRiver(msg2.claim)
    } else {
        println "pass: ${msg2}"
    }
    time = System.nanoTime() - time
    println(String.format("Time: %.1f seconds", time / 1.0e9))
    if (time > 1.0e9) {
        println "TIMEOUT in game phase"
        System.exit(0)
    }
    punter.state = msg2.state
    msg2.remove('state')
    punter.lastMove = msg2
    process.waitFor()
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

def bfs(int source, int target, rivers) {
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
        rivers.each { r ->
            if (r.source == node && !visited.containsKey(r.target)) {
                queue.addLast(r.target)
                visited.put(r.target, node)
            }
            else if (r.target == node && !visited.containsKey(r.source)) {
                queue.addLast(r.source)
                visited.put(r.source, node)
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
punters = args[1..-1].collect { p -> [command: p] }
numPunters = punters.size()

println "Map: ${mapFile}"
println "Punters: ${numPunters}"

map = jsonSlurper.parse(new File(mapFile))
// read again to get a copy of the rivers
rivers = jsonSlurper.parse(new File(mapFile)).rivers

println "Map: ${map.sites.size()} sites, ${rivers.size()} rivers, ${map.mines.size()} mines."


punters.eachWithIndex { p,id ->
    runSetup(p,id)
    println "Punter ${id} initialized: ${p.name}"
}

for (int move = 0;; move++) {
    int open = countUnclaimed(rivers)
    println "Move ${move}, unclaimed: ${open}"
    if (open == 0) {
        break
    }
    int punter = move % numPunters
    runMove(punters[punter])
}

// sending stop seems unnecessary

// println "Final claims: ${rivers}"
punters.each { p ->
    int numRivers = rivers.count { it.owner == p.id }
    println "Score ${p.id}, ${p.name}: ${computeScore(p.id)} (rivers: ${numRivers})"
}