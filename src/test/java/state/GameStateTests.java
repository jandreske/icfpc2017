package state;

import io.Map;
import io.River;
import io.Setup;
import io.Site;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GameStateTests {

    private GameState state;

    @Before
    public void setup() {
        Set<Site> sites = IntStream.range(0, 5).mapToObj(i -> new Site(i)).collect(Collectors.toSet());
        Set<River> rivers = Stream.of(
                new River(0,1),
                new River(1,3),
                new River(3,4),
                new River(0,2),
                new River(2,4),
                new River(0,4))
            .collect(Collectors.toSet());
        Set<Integer> mines = Collections.singleton(4);
        Map map = new Map(sites, rivers, mines);
        Setup.Request setup = new Setup.Request();
        setup.setPunter(1);
        setup.setPunters(2);
        setup.setMap(map);
        state = new GameState(setup);
        state.getRiver(0,1).get().setOwner(1);
        state.getRiver(1,3).get().setOwner(1);
        state.getRiver(3,4).get().setOwner(1);
    }


    @Test
    public void shortestRouteLength() {
        Assert.assertEquals(2, state.getShortestRoute(2,3).size());
    }

    @Test
    public void score() {
        Assert.assertEquals(6, state.getScore(1));
    }
}
