package state;

import io.River;

import java.util.*;

/**
 * Find shortest paths using breadth-first-search.
 */
public class Bfs {

    private static final River SENTINEL = new River(-1,-1);

    private final int maxVertex;
    private final BitSet vertices;

    private final ArrayNatMap<List<River>> edgesByVertex;

    public Bfs(Collection<River> edges) {
        int n = edges.size();
        vertices = new BitSet(n);
        int maxId = -1;
        for (River e : edges) {
            vertices.set(e.getSource());
            vertices.set(e.getTarget());
            if (e.getSource() > maxId) maxId = e.getSource();
            if (e.getTarget() > maxId) maxId = e.getTarget();
        }
        maxVertex = maxId;
        edgesByVertex = new ArrayNatMap<>(maxId + 1);
        for (River e : edges) {
            addEdge(e.getSource(), e);
            addEdge(e.getTarget(), e);
        }

    }

    private void addEdge(int vertex, River edge) {
        List<River> es = edgesByVertex.get(vertex);
        if (es == null) {
            es = new ArrayList<>();
            edgesByVertex.put(vertex, es);
        }
        es.add(edge);
    }

    public boolean containsVertex(int vertex) {
        return vertices.get(vertex);
    }

    /**
     * Find the shortest path from source to target, in order from source.
     * Returns null if there is no path between source and target,
     * and the empty list if source and target are identical.
     */
    public List<River> getShortestPath(int source, int target) {
        if (source == target) {
            return Collections.emptyList();
        }
        if (!(containsVertex(source) && containsVertex(target))) {
            return null;
        }

        ArrayNatMap<River> visited = new ArrayNatMap<>(maxVertex + 1);
        IntQueue queue = new IntQueue(24);
        enqueue(visited, queue, source, SENTINEL);
        while (!queue.isEmpty()) {
            int node = queue.removeFirst();
            if (node == target) {
                return extractPath(visited, node);
            }
            List<River> edges = edgesByVertex.get(node);
            if (edges != null) {
                for (River r : edges) {
                    if (r.getSource() == node && !visited.containsKey(r.getTarget())) {
                        enqueue(visited, queue, r.getTarget(), r);
                    } else if (!visited.containsKey(r.getSource())) {
                        enqueue(visited, queue, r.getSource(), r);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find the distance to all vertices reachable from source.
     * The source is included in the returned map with a value of 0.
     */
    public ArrayNatMap<Integer> getAllShortestPathLengths(int source) {
        ArrayNatMap<Integer> result = new ArrayNatMap<>(maxVertex + 1);
        IntQueue queue = new IntQueue(24);
        int dist = 0;
        enqueue(result, queue, source, dist);
        while (!queue.isEmpty()) {
            dist++;
            int node = queue.removeFirst();
            List<River> edges = edgesByVertex.get(node);
            if (edges != null) {
                for (River r : edges) {
                    if (r.getSource() == node && !result.containsKey(r.getTarget())) {
                        enqueue(result, queue, r.getTarget(), dist);
                    } else if (!result.containsKey(r.getSource())) {
                        enqueue(result, queue, r.getSource(), dist);
                    }
                }
            }
        }
        return result;
    }


    private static void enqueue(ArrayNatMap<Integer> visited, IntQueue queue, int node, int dist) {
        visited.put(node, dist);
        queue.addLast(node);
    }


    private static void enqueue(ArrayNatMap<River> visited, IntQueue queue, int node, River edge) {
        visited.put(node, edge);
        queue.addLast(node);
    }

    private static List<River> extractPath(ArrayNatMap<River> visited, int node) {
        LinkedList<River> path = new LinkedList<>();
        for (;;) {
            River entry = visited.get(node);
            if (entry == SENTINEL) {
                return path;
            }
            path.addFirst(entry);
            node = entry.getOpposite(node);
        }
    }

}
