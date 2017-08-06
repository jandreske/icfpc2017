package state;

import io.River;

import java.util.Collections;
import java.util.List;

class GraphMap {

    private final Bfs bfs;

    GraphMap(Iterable<Integer> sites, Iterable<River> rivers) {
        bfs = new Bfs(rivers);
    }

    /**
     * Return the length of the shortest route or -1 if no route exists.
     */
    int getShortestRouteLength(int a, int b) {
        List<River> path = bfs.getShortestPath(a, b);
        return (path == null) ? -1 : path.size();
    }

    List<River> getShortestRoute(int a, int b) {
        List<River> path = bfs.getShortestPath(a, b);
        if (path == null) {
            return Collections.emptyList();
        }
        return path;
    }

    boolean hasRoute(int a, int b) {
        return bfs.getShortestPath(a, b) != null;
    }

}
