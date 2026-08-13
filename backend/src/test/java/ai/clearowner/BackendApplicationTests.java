package ai.clearowner;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BackendApplicationTests {

    /** The context must start without a reachable database, so the app can report the outage. */
    @Test
    void contextLoads() {
    }
}
