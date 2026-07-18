package com.dashflow.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.Socket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * W0-US03: Flyway baseline creates {@code tenants} against Compose MySQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class FlywayBaselineIT {

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

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void tenantsTable_exists() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'tenants'
            """,
            Integer.class);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void flywaySchemaHistory_hasBaseline() {
    Integer applied =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM flyway_schema_history
            WHERE success = 1
              AND script = 'V1__baseline.sql'
            """,
            Integer.class);
    assertThat(applied).isEqualTo(1);
  }
}
