package ai.clearowner.dto;

import java.util.List;

/** A bounded subgraph shaped for the frontend visualiser. */
public record GraphView(List<Node> nodes, List<Edge> edges) {

    public record Node(String id, String label, String type, String sublabel,
                       boolean flagged, boolean focus) {
    }

    public record Edge(String source, String target, String type, Double percentage) {
    }
}
