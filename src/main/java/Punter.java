import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.*;
import io.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import solvers.*;
import solvers.chris.HeuristicSolver;
import state.GameState;
import state.GameStateFactory;

import java.io.*;

import java.net.Socket;
import java.util.concurrent.*;

public class Punter {

    private static final int TIME_OUT_MS = 500;
    private static final int SOCKET_TIMEOUT_MS = 10 * 60 * 1000;

    private static Solver getSolver(String arg) {
        switch (arg) {
            case "random":          return new RandomClaimer();
            case "simple":          return new SimpleMineClaimer();
            case "maxpoint":        return new MaxPointClaimer();
            case "expanding":       return new ExpandingMineClaimer();
            case "connect":         return new MineConnectClaimer();
            case "splurgefly":      return new SplurgeFly();
            case "splurgefly44":    return new SplurgeFly(4, 4);
            case "splurgefly53":    return new SplurgeFly(5, 3);
            default:                return new SplurgeFly();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(Punter.class);
    private final ExecutorService timeoutExecutorService;

    public static void main(String[] args) {
        boolean online = false;
        String server = "";
        int port = 0;
        Solver solver = getSolver("default");
        if (args.length > 0) {
            solver = getSolver(args[0]);
        }

        if (args.length > 1) {
            server = "punter.inf.ed.ac.uk";
            port = Integer.parseInt(args[1]);
            online = true;
        }

        if (online) {
            startOnlineGame(server, port, solver);
        } else {
            runOfflineRound(solver);
        }
    }

    // for non-forking offline server use
    public static void runOffline(String solver, InputStream in, PrintStream out) throws IOException {
        Punter punter = new Punter(getSolver(solver));
        try {
            punter.runOfflineMove(in, out);
        } finally {
            punter.timeoutExecutorService.shutdownNow();
        }
    }

    private static void runOfflineRound(Solver solver) {
        Punter punter = new Punter(solver);
        try {
            punter.runOfflineMove(System.in, System.out);
        } catch (IOException e) {
            LOG.error("Unexpected Exception: ", e);
        } finally {
            punter.timeoutExecutorService.shutdownNow();
        }
    }

    private static void startOnlineGame(String server, int port, Solver solver) {
        Scoring.Data scoring = null;
        Punter punter = null;

        try (Socket client = new Socket(server, port)) {
            client.setSoTimeout(SOCKET_TIMEOUT_MS);
            InputStream input = new BufferedInputStream(client.getInputStream());
            OutputStream output = client.getOutputStream();
            punter = new Punter(solver);
            scoring = punter.runOnlineGame(input, new PrintStream(new BufferedOutputStream(output)));
            output.close();
            input.close();

        } catch (IOException e) {
            LOG.error("Unexpected Exception: ", e);
        }
        finally {
            if (punter != null) {
                punter.timeoutExecutorService.shutdownNow();
            }
        }

        LOG.info("SCORE on {}: {}", port, scoring == null ? "FAILED" : scoring);
    }


    private final ObjectMapper objectMapper;
    private final Solver solver;
    private final GameStateFactory gameStateFactory;

    public static class NamedThreadFactory implements ThreadFactory {

        private final String prefix;
        private volatile int count = 1;

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix + "-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(prefix + count);
            count++;
            return t;
        }
    }

    private Punter(Solver solver) {
        timeoutExecutorService =
           new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, new SynchronousQueue<>());
        ((ThreadPoolExecutor) timeoutExecutorService).setThreadFactory(new NamedThreadFactory("ASoM-timeout"));
        gameStateFactory = new GameStateFactory();
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule module = new SimpleModule("ASoMModel", Version.unknownVersion());
        SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
        resolver.addMapping(GameState.class, gameStateFactory.getImplementationClass());
        module.setAbstractTypes(resolver);
        objectMapper.registerModule(module);
        this.solver = solver;
    }

    private Scoring.Data runOnlineGame(InputStream in, PrintStream out) throws IOException {
        try (PrintWriter record = new PrintWriter(new FileWriter("record.json"))) {
            record.println("{");
            // 0. Handshake
            LOG.info("Starting Handshake...");
            Handshake.Request request = new Handshake.Request("A Storm of Minds: " + solver.getName());
            writeJson(out, request);
            Handshake.Response response = readJson(in, Handshake.Response.class);
            if (!response.getYou().equals(request.getMe())) {
                throw new ProtocolException("Handshake: Name did not match [request: '" + request.getMe() + "', response: '" + response.getYou() + "']");
            }
            LOG.info("Handshake completed");

            // 1. Setup
            LOG.info("Receiving setup...");
            Setup.Request setup = readJson(in, Setup.Request.class);
            GameState state = gameStateFactory.create(setup);
            Setup.Response setupResponse = new Setup.Response(setup.getPunter());
            if (setup.getSettings() != null && setup.getSettings().isFutures()) {
                Future[] futures = solver.getFutures(state);
                setupResponse.setFutures(futures);
                state.setFutures(futures);
            }
            writeJson(out, setupResponse);
            LOG.info("Sent ready message: {}", objectMapper.writeValueAsString(setupResponse));
            LOG.info("Punter id: {}", setup.getPunter());
            LOG.info("Number of punters: {}", setup.getPunters());
            //LOG.info("Map: {}", objectMapper.writeValueAsString(setup.getMap()));
            LOG.info("STATS: {} sites, {} rivers, {} mines", setup.getMap().getSites().size(),
                    setup.getMap().getRivers().size(), setup.getMap().getMines().size());
            record.println("\"setup\":");
            record.println(objectMapper.writeValueAsString(setup));

            // 2. Gameplay
            record.println(", \"moves\": ");
            char recordSep = '[';
            int numPunters = state.getNumPunters();
            int punterId = state.getMyPunterId();
            while (state.getRemainingNumberOfMoves() > 0) {
                Gameplay.Request moveRequest = readJson(in, Gameplay.Request.class);
                moveRequest.getMove().moves.forEach(state::applyMove);
                for (Move move : moveRequest.getMove().moves) {
                    Move.ClaimData claim = move.getClaim();
                    if (claim != null) {
                        record.print(recordSep);
                        recordSep = ',';
                        record.println(objectMapper.writeValueAsString(claim));
                    }
                }

                Move move = getNextMoveWithTimeout(state, TIME_OUT_MS);
                if (move == null) move = Move.pass(state.getMyPunterId());
                state.movePerformed();
                writeJson(out, move);
                Move.ClaimData claim1 = move.getClaim();
                if (claim1 != null) {
                    record.print(recordSep);
                    recordSep = ',';
                    record.println(objectMapper.writeValueAsString(claim1));
                }
                LOG.info("sent move: {}\n", objectMapper.writeValueAsString(move));
            }

            LOG.info("Receiving scoring info...");
            Scoring.Data scoring = readJson(in, Scoring.class).stop;
            scoring.moves.forEach(state::applyMove);
            LOG.info("number of own rivers: {}", state.getOwnRivers().size());
            int myScore = scoring.scores.stream().filter(score -> score.punter == punterId).findFirst().get().score;
            int rank = numPunters - (int) scoring.scores.stream().filter(score -> score.score < myScore).count();
            LOG.info("RANKING {} / {} with {} points", rank, numPunters, myScore);
//            scoring.scores.forEach(score -> {
//                int computed = state.getScore(score.punter);
//                if (score.score != computed) {
//                    LOG.warn("score mismatch. Server: {}, computed: {}", score.score, computed);
//                }
//            });
            record.println("]}");
            return scoring;
        }
    }

    private Move getNextMoveWithTimeout(GameState state, int timeOutMs) {
        java.util.concurrent.Future<Move> future = timeoutExecutorService.submit(() -> solver.getNextMove(state));
        try {
            return future.get(timeOutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //something interrupted, probably your service is shutting down
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            //error happened while executing
            LOG.error("Unexpected exception", e);
            return Move.pass(state.getMyPunterId());
        } catch (TimeoutException e) {
            LOG.warn("Timeout, taking best option so far");
            future.cancel(true);
            return solver.getBestChoice();
        }
    }

    private void runOfflineMove(InputStream in, PrintStream out) throws IOException {

        // 0. Handshake
        LOG.info("Starting Handshake...");
        Handshake.Request request = new Handshake.Request("A Storm of Minds: " + solver.getName());
        writeJson(out, request);
        Handshake.Response response = readJson(in, Handshake.Response.class);
        if (!response.getYou().equals(request.getMe())) {
            throw new ProtocolException("Handshake: Name did not match [request: '" + request.getMe() + "', response: '" + response.getYou() + "']");
        }
        LOG.info("Handshake completed");

        LOG.info("Receiving...");
        Object req = readOfflineRequest(in);
        if (req instanceof Setup.Request) {
            Setup.Request setup = (Setup.Request) req;
            LOG.info("Received setup request");
            GameState state = gameStateFactory.create(setup);
            Setup.Response setupResponse = new Setup.Response(setup.getPunter());
            setupResponse.setState(state);
            if (setup.getSettings() != null && setup.getSettings().isFutures()) {
                Future[] futures = solver.getFutures(state);
                setupResponse.setFutures(futures);
                state.setFutures(futures);
            }
            writeJson(out, setupResponse);
            LOG.info("Punter id: {}", setup.getPunter());
            LOG.info("Number of punters: {}", setup.getPunters());
            LOG.info("Map: {}", objectMapper.writeValueAsString(setup.getMap()));
        } else if (req instanceof Gameplay.Request) {
            Gameplay.Request moveRequest = (Gameplay.Request) req;
            GameState state = moveRequest.getState();
            if (state == null) {
                throw new ProtocolException("state not supplied in offline mode");
            }
            moveRequest.getMove().moves.forEach(state::applyMove);
            Move move = getNextMoveWithTimeout(state, TIME_OUT_MS);
            if (move == null) move = Move.pass(state.getMyPunterId());
            state.movePerformed();
            move.setState(state);
            writeJson(out, move);
            LOG.info("Move and new state: {}", move);
        } else {
            Scoring scoring = (Scoring) req;
            LOG.info("Scoring received: {}", scoring);
        }
    }

    private void writeJson(PrintStream out, Object value) throws JsonProcessingException {
        String s = objectMapper.writeValueAsString(value);
        out.print(s.length());
        out.print(':');
        out.print(s);
        out.flush();
    }

    private <T> T readJson(InputStream in, Class<T> clazz) throws IOException {
        int length = decodeLength(in);
        byte[] data = readBytes(in, length);
        LOG.info("received: {}", new String(data));
        return objectMapper.readValue(data, clazz);
    }

    private Object readOfflineRequest(InputStream in) throws IOException {
        int length = decodeLength(in);
        byte[] data = readBytes(in, length);
        Class<?> clazz;
        // HACK
        if (contains(data, "\"move\"")) {
            clazz = Gameplay.Request.class;
        } else if (contains(data, "\"stop\"")) {
            clazz = Scoring.class;
        } else {
            clazz = Setup.Request.class;
        }
        return objectMapper.readValue(data, clazz);
    }

    private static boolean contains(byte[] data, String needle) {
        int n = data.length;
        int m = needle.length();
        for (int p = 0;;) {
            p = indexOf(data, needle.charAt(0), p);
            if (p < 0) {
                return false;
            }
            boolean match = true;
            for (int i = 1; i < m && p + i < n; i++) {
                if (data[p+i] != needle.charAt(i)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
            p++;
        }
    }

    private static int indexOf(byte[] data, int value, int startOffset) {
        int n = data.length;
        for (int i = startOffset; i < n; i++) {
            if (data[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] readBytes(InputStream in, int length) throws IOException {
        byte[] data = new byte[length];
        for (int n = 0; n < length; ) {
            int m = in.read(data, n, length - n);
            if (m < 0) {
                throw new ProtocolException("unexpected end of input at " + n);
            }
            n += m;
        }
        return data;
    }

    private static int decodeLength(InputStream in) throws IOException {
        int length = 0;
        for (;;) {
            int c = in.read();
            if (c == ':') {
                break;
            }
            length = 10 * length + Character.getNumericValue(c);
        }
        return length;
    }

}
