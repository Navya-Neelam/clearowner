package ai.clearowner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for CognoDB. Values come from environment variables
 * (COGNODB_URI, COGNODB_USER, COGNODB_PASSWORD) and are never committed.
 */
@ConfigurationProperties(prefix = "cognodb")
public record CognoDbProperties(String uri, String user, String password) {
}
