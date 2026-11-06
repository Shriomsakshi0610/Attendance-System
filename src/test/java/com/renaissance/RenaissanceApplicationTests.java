package com.renaissance;

import com.renaissance.config.MongoTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Import(MongoTestConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class RenaissanceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring ApplicationContext starts cleanly
    }
}
