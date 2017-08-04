import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.Handshake;
import io.ProtocolException;
import io.Scoring;
import io.Setup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import java.net.Socket;

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
        System.out.println("Offline mode not yet implemented.");
    }

    private static void startOnlineGame(String server, int port) {
        try (Socket client = new Socket(server, port)) {
            DataInputStream input = new DataInputStream(client.getInputStream());
            DataOutputStream output = new DataOutputStream(client.getOutputStream());

            new Punter().runGame(input, new PrintStream(output));

            output.close();
            input.close();
        } catch (IOException e) {
            LOG.error("Unexpected Exception: ", e);
            throw new RuntimeException(e);
        }
    }


    private final ObjectMapper objectMapper;

    private Punter() {
        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    void runGame(InputStream in, PrintStream out) throws IOException {

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
        Setup.Response setupResponse = new Setup.Response(setup.getPunter());
        writeJson(out, setupResponse);
        LOG.info("Punter id: " + setup.getPunter());
        LOG.info("Number of punters: " + setup.getPunters());
        LOG.info("Map: " + objectMapper.writeValueAsString(setup.getMap()));

        // 2. Gameplay
        int numRivers = setup.getMap().getRivers().size();
        int numPunters = setup.getPunters();
        int punterId = setup.getPunter();
        int ownMoves = (numRivers / numPunters) + ((numRivers % numPunters) > punterId ? 1 : 0);
        for (int moveNum = 0; moveNum < ownMoves; moveNum++) {
            // TODO: a move
        }

        LOG.info("Receiving scoring info...");
        Scoring scoring = readJson(in, Scoring.class);
        // TODO
    }

    private void writeJson(PrintStream out, Object value) throws JsonProcessingException {
        String s = objectMapper.writeValueAsString(value);
        out.print(s.length());
        out.print(':');
        out.print(s);
        out.flush();
    }

    private <T> T readJson(InputStream in, Class<T> clazz) throws IOException {
        int length = 0;
        for (;;) {
            int c = in.read();
            if (c == ':') {
                break;
            }
            length = 10 * length + Character.getNumericValue(c);
        }
        return objectMapper.readValue(in, clazz);
    }
}
