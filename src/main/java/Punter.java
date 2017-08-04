import com.fasterxml.jackson.databind.ObjectMapper;
import io.Handshake;
import io.ProtocolException;
import io.Scoring;
import io.Setup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Punter {

    private static final Logger LOG = LoggerFactory.getLogger(Punter.class);

    public static void main(String[] args) {
        System.out.println("Hello Punter!");
    }


    private final ObjectMapper objectMapper;

    private Punter() {
        objectMapper = new ObjectMapper();
    }

    void runGame(InputStream in, OutputStream out) throws IOException {

        // 0. Handshake
        LOG.info("Starting Handshake...");
        Handshake.Request request = new Handshake.Request("A Storm of Minds");
        objectMapper.writeValue(out, request);
        out.flush();
        Handshake.Response response = objectMapper.readValue(in, Handshake.Response.class);
        if (!response.getYou().equals(request.getMe())) {
            throw new ProtocolException("Handshake: Name did not match [request: '" + request.getMe() + "', response: '" + response.getYou() + "']");
        }
        LOG.info("Handshake completed");

        // 1. Setup
        LOG.info("Receiving setup...");
        Setup.Request setup = objectMapper.readValue(in, Setup.Request.class);
        Setup.Response setupResponse = new Setup.Response(setup.getPunter());
        objectMapper.writeValue(out, setupResponse);
        out.flush();
        LOG.info("Punter id: " + setup.getPunter());
        LOG.info("Number of punters: " + setup.getPunters());
        LOG.info("Map: " + objectMapper.writeValueAsString(setup.getMap()));

        // 2. Gameplay
        int numRivers = setup.getMap().getRivers().size();
        for (int moveNum = 0; moveNum < numRivers; moveNum++) {
            // TODO: a move
        }

        LOG.info("Receiving scoring info...");
        Scoring scoring = objectMapper.readValue(in, Scoring.class);
        // TODO
    }
}
