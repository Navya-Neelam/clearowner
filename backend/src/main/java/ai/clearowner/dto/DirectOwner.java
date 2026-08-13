package ai.clearowner.dto;

/** A shareholder as the register records it - one hop, no inference. */
public record DirectOwner(
        String type,
        String id,
        String name,
        double percentage,
        String shareClass,
        String since) {
}
