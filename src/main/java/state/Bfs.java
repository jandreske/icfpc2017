package state;

import io.River;

import java.util.*;

public class Bfs {

    private static final River SENTINEL = new River(-1,-1);

    private final int maxVertex;
    private final Set<Integer> vertices;

    private final ArrayNatMap<List<River>> edgesByVertex;

    public Bfs(Collection<River> edges) {
        int n = edges.size();
        vertices = new HashSet<>(n);
        int maxId = -1;
        for (River e : edges) {
            vertices.add(e.getSource());
            vertices.add(e.getTarget());
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

    public List<River> getShortestPath(int source, int target) {
        if (source == target) {
            return Collections.emptyList();
        }
        if (!(vertices.contains(source) && vertices.contains(target))) {
            return null;
        }

        ArrayNatMap<River> visited = new ArrayNatMap<>(maxVertex + 1);
        // Map<Integer, River> visited = new HashMap<>();
        IntQueue queue = new IntQueue(24);
        // LinkedList<Integer> queue = new LinkedList<>();
        visited.put(source, SENTINEL);
        queue.addLast(source);
        while (!queue.isEmpty()) {
            int node = queue.removeFirst();
            if (node == target) {
                return extractPath(visited, node);
            }
            List<River> edges = edgesByVertex.get(node);
            if (edges != null) {
                for (River r : edges) {
                    if (r.getSource() == node && !visited.containsKey(r.getTarget())) {
                        queue.addLast(r.getTarget());
                        visited.put(r.getTarget(), r);
                    } else if (r.getTarget() == node && !visited.containsKey(r.getSource())) {
                        queue.addLast(r.getSource());
                        visited.put(r.getSource(), r);
                    }
                }
            }
        }
        return null;
    }

    private List<River> extractPath(ArrayNatMap<River> visited, int node) {
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

    private List<River> extractPath(Map<Integer, River> visited, int node) {
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
