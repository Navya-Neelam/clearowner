package ai.clearowner.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Builds the single shared Driver. The driver is thread-safe and manages its own
 * connection pool, so one instance serves the whole application.
 * <p>
 * Note we do not verify connectivity at startup: the app must still boot when
 * CognoDB is unreachable so it can report the failure through /api/health
 * rather than crash-looping on the host.
 */
@Configuration
@EnableConfigurationProperties(CognoDbProperties.class)
public class Neo4jConfig {

    private static final Logger log = LoggerFactory.getLogger(Neo4jConfig.class);

    @Bean(destroyMethod = "close")
    public Driver neo4jDriver(CognoDbProperties props) {
        if (props.uri() == null || props.uri().isBlank()) {
            throw new IllegalStateException(
                    "COGNODB_URI is not set. Copy .env.example to .env and provide connection details.");
        }
        log.info("Configuring Neo4j driver for {}", props.uri());

        Config config = Config.builder()
                // The c0 free tier allows 200 connections; stay well under it.
                .withMaxConnectionPoolSize(20)
                .withConnectionAcquisitionTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .withConnectionTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .withMaxTransactionRetryTime(10, java.util.concurrent.TimeUnit.SECONDS)
                .withLogging(org.neo4j.driver.Logging.slf4j())
                .build();

        return GraphDatabase.driver(
                props.uri(), AuthTokens.basic(props.user(), props.password()), config);
    }
}
