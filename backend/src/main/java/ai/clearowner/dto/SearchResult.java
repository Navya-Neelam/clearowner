package ai.clearowner.dto;

/** A company or person matched by the global search box. */
public record SearchResult(String type, String id, String name, String subtitle) {
}
