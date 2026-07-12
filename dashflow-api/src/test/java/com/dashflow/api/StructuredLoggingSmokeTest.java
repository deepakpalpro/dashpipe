package com.dashflow.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.Socket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * W0-US04: structured logging config is present (JSON/Logstash console format via Boot).
 */
@SpringBootTest
@ActiveProfiles("local")
class StructuredLoggingSmokeTest {

  @BeforeAll
  static void requireComposeMysql() {
    assumeTrue(
        isPortOpen("127.0.0.1", 3306),
        "Compose MySQL is not reachable on localhost:3306 — run: docker compose up -d mysql");
  }

  private static boolean isPortOpen(String host, int port) {
    try (Socket socket = new Socket(host, port)) {
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  @Autowired private Environment environment;

  @Test
  void structuredConsoleFormat_isLogstash() {
    assertThat(environment.getProperty("logging.structured.format.console")).isEqualTo("logstash");
    assertThat(environment.getProperty("spring.application.name")).isEqualTo("dashflow-api");
  }

  @Test
  void logbackSpringXml_isOnClasspath() {
    assertThat(getClass().getClassLoader().getResource("logback-spring.xml")).isNotNull();
  }
}
