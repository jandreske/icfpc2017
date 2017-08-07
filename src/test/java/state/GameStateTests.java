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
        River[] riversA = new River[]{
                new River(0,1),
                new River(1,3),
                new River(3,4),
                new River(0,2),
                new River(2,4),
                new River(0,4)
        };
        riversA[0].setOwner(1);
        riversA[1].setOwner(1);
        riversA[2].setOwner(1);
        Set<River> rivers = Stream.of(riversA)
            .collect(Collectors.toSet());
        Set<Integer> mines = Collections.singleton(4);
        Map map = new Map(sites, rivers, mines);
        Setup.Request setup = new Setup.Request();
        setup.setPunter(1);
        setup.setPunters(2);
        setup.setMap(map);
        state = new MapBasedGameState(setup);
    }

    @Test
    public void shortestRouteSelfIsEmpty() {
        Assert.assertTrue(state.getShortestRoute(2, 2).isEmpty());
    }

    @Test
    public void shortestRouteLength() {
        Assert.assertEquals(2, state.getShortestRoute(2,3).size());
        Assert.assertEquals(1, state.getShortestRoute(0,4).size());
        Assert.assertEquals(1, state.getShortestRoute(4,0).size());
    }

    @Test
    public void canReach() {
        Assert.assertFalse(state.canReach(1, 2, 3));
        Assert.assertTrue(state.canReach(1, 1, 4));
    }

    @Test
    public void score() {
        Assert.assertEquals(6, state.getScore(1));
    }

    @Test
    public void allRouteLengths3() {
        GraphMap map = ((MapBasedGameState) state).getGraphMap();

        ArrayNatMap<Integer> r = map.getAllShortestRouteLengths(3);
        Assert.assertEquals(5, r.size());
        Assert.assertEquals(0, (int) r.get(3));
        Assert.assertEquals(2, (int) r.get(0));
        Assert.assertEquals(1, (int) r.get(1));
        Assert.assertEquals(2, (int) r.get(2));
        Assert.assertEquals(1, (int) r.get(4));

        r = map.getAllShortestRouteLengths(4);
        Assert.assertEquals(5, r.size());
        Assert.assertEquals(0, (int) r.get(4));
        Assert.assertEquals(1, (int) r.get(0));
        Assert.assertEquals(2, (int) r.get(1));
        Assert.assertEquals(1, (int) r.get(2));
        Assert.assertEquals(1, (int) r.get(3));
    }
}
