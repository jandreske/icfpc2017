import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import solvers.*;
import solvers.chris.HeuristicSolver;
import state.GameState;

import java.io.*;

import java.net.Socket;
import java.util.concurrent.*;

public class Punter {

    private static Solver getSolver(String arg) {
        switch (arg) {
            case "random":      return new RandomClaimer();
            case "simple":      return new SimpleMineClaimer();
            case "maxpoint":    return new MaxPointClaimer();
            case "expanding":   return new ExpandingMineClaimer();
            case "connect":     return new MineConnectClaimer();
            case "heuristic":   return new HeuristicSolver();
            default:            return new MineConnectClaimer();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(Punter.class);
    private final ExecutorService timeoutExecutorService = Executors.newSingleThreadExecutor();

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

    private static void runOfflineRound(Solver solver) {
        try {
            Punter punter = new Punter(solver);
            punter.runOfflineMove(System.in, System.out);
        } catch (IOException e) {
            LOG.error("Unexpected Exception: ", e);
        }
    }

    private static void startOnlineGame(String server, int port, Solver solver) {
        Scoring.Data scoring = null;
        try (Socket client = new Socket(server, port)) {
            client.setSoTimeout(60000);
            InputStream input = client.getInputStream();
            OutputStream output = client.getOutputStream();
            Punter punter = new Punter(solver);
            scoring = punter.runOnlineGame(input, new PrintStream(output));
            output.close();
            input.close();

        } catch (IOException e) {
            LOG.error("Unexpected Exception: ", e);
        }

        LOG.info("SCORE on {}: {}", port, scoring == null ? "FAILED" : scoring);
    }


    private final ObjectMapper objectMapper;
    private final Solver solver;

    private Punter(Solver solver) {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
            GameState state = new GameState(setup);
            Setup.Response setupResponse = new Setup.Response(setup.getPunter());
            if (setup.getSettings() != null && setup.getSettings().getFutures()) {
                setupResponse.setFutures(solver.getFutures(state));
            }
            writeJson(out, setupResponse);
            LOG.info("Sent ready message: {}", objectMapper.writeValueAsString(setupResponse));
            LOG.info("Punter id: {}", setup.getPunter());
            LOG.info("Number of punters: {}", setup.getPunters());
            LOG.info("Map: {}", objectMapper.writeValueAsString(setup.getMap()));
            record.println("\"setup\":");
            record.println(objectMapper.writeValueAsString(setup));

            // 2. Gameplay
            record.println(", \"moves\": ");
            char recordSep = '[';
            int numRivers = setup.getMap().getRivers().size();
            int numPunters = setup.getPunters();
            int punterId = setup.getPunter();
            int ownMoves = (numRivers / numPunters) + ((numRivers % numPunters) > punterId ? 1 : 0);
            for (int moveNum = 0; moveNum < ownMoves; moveNum++) {
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

                River claim = getNextMoveWithTimeout(state, 900);

                Move move = (claim == null) ? Move.pass(state.getMyPunterId())
                        : Move.claim(state.getMyPunterId(), claim);
                writeJson(out, move);
                state.applyMove(move);
                Move.ClaimData claim1 = move.getClaim();
                if (claim1 != null) {
                    record.print(recordSep);
                    recordSep = ',';
                    record.println(objectMapper.writeValueAsString(claim1));
                }
                LOG.info("sent move: {}", objectMapper.writeValueAsString(move));
            }

            LOG.info("Receiving scoring info...");
            Scoring.Data scoring = readJson(in, Scoring.class).stop;
            scoring.moves.forEach(state::applyMove);
            LOG.info("number of own rivers: {}", state.getOwnRivers().size());
            int myScore = scoring.scores.stream().filter(score -> score.punter == punterId).findFirst().get().score;
            int rank = numPunters - (int) scoring.scores.stream().filter(score -> score.score < myScore).count();
            LOG.info("RANKING {} / {}", rank, numPunters);
            scoring.scores.forEach(score -> {
                int computed = state.getScore(score.punter);
                if (score.score != computed) {
                    LOG.warn("score mismatch. Server: {}, computed: {}", score.score, computed);
                }
            });
            record.println("]}");
            timeoutExecutorService.shutdown();
            return scoring;
        }
    }

    private River getNextMoveWithTimeout(GameState state, int timeOutMs) {
        java.util.concurrent.Future<River> future = timeoutExecutorService.submit(() -> solver.getNextMove(state));
        try {
            return future.get(timeOutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //something interrupted, probably your service is shutting down
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            //error happened while executing
            LOG.error("Unexpected exception", e);
            return null;
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
        Object req = readOneOf(in, Gameplay.Request.class, Setup.Request.class, Scoring.class);
        if (req instanceof Setup.Request) {
            Setup.Request setup = (Setup.Request) req;
            LOG.info("Received setup request");
            GameState state = new GameState(setup);
            Setup.Response setupResponse = new Setup.Response(setup.getPunter());
            setupResponse.setState(state);
            if (setup.getSettings() != null && setup.getSettings().getFutures()) {
                setupResponse.setFutures(solver.getFutures(state));
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
            River claim = getNextMoveWithTimeout(state, 650);
            Move move = (claim == null) ? Move.pass(state.getMyPunterId())
                    : Move.claim(state.getMyPunterId(), claim);
            state.applyMove(move);
            move.setState(state);
            writeJson(out, move);
            LOG.info("Move and new state: {}", move);
        } else {
            Scoring scoring = (Scoring) req;
            LOG.info("Scoring received: {}", scoring);
        }
        timeoutExecutorService.shutdown();
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

    private Object readOneOf(InputStream in, Class<?>... classes) throws IOException {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        int length = decodeLength(in);
        byte[] data = readBytes(in, length);
        for (Class<?> clazz : classes) {
            try {
                return objectMapper.readValue(data, clazz);
            }
            catch (JsonMappingException ex) {
                LOG.debug("expected exception trying JSON", ex);
                // continue
            }
        }
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.readValue(data, classes[0]);
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
