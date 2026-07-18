package com.dashpipe.api.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;

/** Loads deterministic tenant fixtures from {@code classpath:fixtures/tenants/}. */
public final class TenantFixtures {

  public static final String T001 = "T001";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private TenantFixtures() {}

  public static JsonNode loadT001() {
    return load("tenants/t001.json");
  }

  public static JsonNode load(String relativePath) {
    String classpath = "fixtures/" + relativePath;
    try (InputStream in = TenantFixtures.class.getClassLoader().getResourceAsStream(classpath)) {
      if (in == null) {
        throw new IllegalArgumentException("Missing fixture on classpath: " + classpath);
      }
      return MAPPER.readTree(in);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to read fixture: " + classpath, ex);
    }
  }
}
