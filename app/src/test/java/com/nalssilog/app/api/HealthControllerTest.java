package com.nalssilog.app.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

@SuppressWarnings("java:S5960")
class HealthControllerTest {

    @Test
    void exposesBuildVersionWithHealthStatus() {
        String expectedVersion = "test-version";
        Properties properties = new Properties();

        properties.setProperty("version", expectedVersion);
        HealthController controller =
                new HealthController(new BuildProperties(properties));

        HealthController.HealthResponse response = controller.health();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.version()).isEqualTo(expectedVersion);
    }
}
