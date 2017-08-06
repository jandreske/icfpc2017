package state;

import io.River;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.SimpleGraph;

import java.util.Collections;
import java.util.List;

class GraphMap {

    private final SimpleGraph<Integer, River> g;

    private final ShortestPathAlgorithm<Integer, River> spa;

    GraphMap(Iterable<Integer> sites, Iterable<River> rivers) {
        g = new SimpleGraph<>(River.class);
        sites.forEach(site -> g.addVertex(site));
        rivers.forEach(river -> g.addEdge(river.getSource(), river.getTarget(), river));

        // spa = new AStarShortestPath<>(g, (v,t) -> (v == t) ? 0 : 1);
        spa = new DijkstraShortestPath<Integer, River>(g);
    }

    /**
     * Return the length of the shortest route or -1 if no route exists.
     */
    int getShortestRouteLength(int a, int b) {
        GraphPath<Integer, River> path = spa.getPath(a, b);
        return (path == null) ? -1 : path.getLength();
    }

    List<River> getShortestRoute(int a, int b) {
        GraphPath<Integer, River> path = spa.getPath(a, b);
        if (path == null) {
            return Collections.emptyList();
        }
        return path.getEdgeList();
    }

    boolean hasRoute(int a, int b) {
        return spa.getPathWeight(a, b) < Double.POSITIVE_INFINITY;
    }

    /**
     * Get the number of rivers at this site.
     */
    int getNumberOfRivers(int site) {
        return g.degreeOf(site);
    }

}
