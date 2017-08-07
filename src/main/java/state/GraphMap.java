package state;

import io.River;

import java.util.*;

class GraphMap {

    private final Bfs bfs;

    GraphMap(Iterable<Integer> sites, Collection<River> rivers) {
        bfs = new Bfs(rivers);
    }

    /**
     * Return the length of the shortest route or -1 if no route exists.
     */
    int getShortestRouteLength(int a, int b) {
        List<River> path = getShortestPath(a, b);
        return (path == null) ? -1 : path.size();
    }

    List<River> getShortestRoute(int a, int b) {
        List<River> path = getShortestPath(a, b);
        if (path == null) {
            return Collections.emptyList();
        }
        return path;
    }

    boolean hasRoute(int a, int b) {
        return containsSite(a) && containsSite(b) && getShortestPath(a, b) != null;
    }

    boolean containsSite(int site) {
        return bfs.containsVertex(site);
    }

    private List<River> getShortestPath(int a, int b) {
        if (a == b) {
            return Collections.emptyList();
        }
        River key = new River(a,b); // NOT a river
        List<River> path = cache.get(key);
        if (path == null) {
            path = bfs.getShortestPath(a, b);
            if (path == null) {
                cache.put(key, Collections.emptyList());
            } else {
                cache.put(key, path);
            }
            return path;
        }
        return path.isEmpty() ? null : path;
    }

    // uses empty list to represent no path!
    private final Map<River, List<River>> cache = new HashMap<>();

}
