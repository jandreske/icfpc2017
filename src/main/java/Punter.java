import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import solvers.*;
import state.GameState;

import java.io.*;

import java.net.Socket;
import java.util.Arrays;

public class Punter {

    private static final Logger LOG = LoggerFactory.getLogger(Punter.class);

    public static void main(String[] args) {
        boolean online = false;
        String server = "";
        int port = 0;

        if (args.length > 0) {
            server = "punter.inf.ed.ac.uk";
            port = Integer.parseInt(args[0]);
            online = true;
        }

        if (online) {
            startOnlineGame(server, port);
        } else {
            runOfflineRound();
        }
    }

    private static void runOfflineRound() {
        try {
            Punter punter = new Punter();
            punter.runOfflineMove(System.in, System.out);
        } catch (IOException e) {
            LOG.error("Unexpected Exception: ", e);
        }
    }

    private static void startOnlineGame(String server, int port) {
        Scoring.Data scoring = null;
        try (Socket client = new Socket(server, port)) {
            client.setSoTimeout(60000);
            InputStream input = client.getInputStream();
            OutputStream output = client.getOutputStream();
            Punter punter = new Punter();
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

    private Punter() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        solver = new ExpandingMineClaimer();
    }

    private Scoring.Data runOnlineGame(InputStream in, PrintStream out) throws IOException {

        // 0. Handshake
        LOG.info("Starting Handshake...");
        Handshake.Request request = new Handshake.Request("A Storm of Minds");
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
        if (setup.getSettings().getFutures()) {
            setupResponse.setFutures(solver.getFutures(state));
        }
        writeJson(out, setupResponse);
        LOG.info("Sent ready message: {}", objectMapper.writeValueAsString(setupResponse));
        LOG.info("Punter id: {}", setup.getPunter());
        LOG.info("Number of punters: {}", setup.getPunters());
        LOG.info("Map: {}", objectMapper.writeValueAsString(setup.getMap()));

        // 2. Gameplay
        int numRivers = setup.getMap().getRivers().size();
        int numPunters = setup.getPunters();
        int punterId = setup.getPunter();
        int ownMoves = (numRivers / numPunters) + ((numRivers % numPunters) > punterId ? 1 : 0);
        for (int moveNum = 0; moveNum < ownMoves; moveNum++) {
            Gameplay.Request moveRequest = readJson(in, Gameplay.Request.class);
            moveRequest.getMove().moves.forEach(state::applyMove);
            River claim = solver.getNextMove(state);
            Move move = (claim == null) ? Move.pass(state.getMyPunterId())
                                        : Move.claim(state.getMyPunterId(), claim);
            writeJson(out, move);
            state.applyMove(move);
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
        return scoring;
    }

    private void runOfflineMove(InputStream in, PrintStream out) throws IOException {

        // 0. Handshake
        LOG.info("Starting Handshake...");
        Handshake.Request request = new Handshake.Request("A Storm of Minds");
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
            if (setup.getSettings().getFutures()) {
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
            River claim = solver.getNextMove(state);
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
        int length = decodeLength(in);
        byte[] data = readBytes(in, length);
        for (Class<?> clazz : classes) {
            try {
                return objectMapper.readValue(data, clazz);
            }
            catch (JsonMappingException ex) {
                // continue
            }
        }
        throw new ProtocolException("expected one of " + Arrays.toString(classes));
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
