package state;

import io.River;

import java.util.*;

public class Bfs {

    private final Set<Integer> vertices;
    private final Set<River> edges;

    private final Map<Integer, List<River>> edgesByVertex;

    public Bfs(Iterable<River> edges) {
        this.edges = new HashSet<>(200);
        vertices = new HashSet<>(200);
        edgesByVertex = new HashMap<>(200);
        for (River e : edges) {
            this.edges.add(e);
            vertices.add(e.getSource());
            vertices.add(e.getTarget());
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

    public int size() {
        return edges.size();
    }

    public List<River> getShortestPath(int source, int target) {
        if (source == target) {
            return Collections.emptyList();
        }
        if (!(vertices.contains(source) && vertices.contains(target))) {
            return null;
        }

        Map<Integer, River> visited = new HashMap<>(edges.size());
        LinkedList<Integer> queue = new LinkedList<>();
        visited.put(source, null);
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

    private List<River> extractPath(Map<Integer, River> visited, int node) {
        LinkedList<River> path = new LinkedList<>();
        for (;;) {
            River entry = visited.get(node);
            if (entry == null) {
                return path;
            }
            path.addFirst(entry);
            node = entry.getOpposite(node);
        }
    }

}
