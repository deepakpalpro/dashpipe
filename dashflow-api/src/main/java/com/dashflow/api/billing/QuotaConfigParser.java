package com.dashflow.api.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Parses {@code tenants.quota_config}.
 *
 * <pre>
 * {
 *   "dimensions": {
 *     "platform.pipeline_runs": { "soft": 100, "hard": 200 },
 *     "data.records_processed": { "soft": 1000000, "hard": 2000000 }
 *   }
 * }
 * </pre>
 *
 * Missing soft/hard for a dimension means no limit on that side. Null/blank/invalid → empty config
 * (credit check only).
 */
@Component
public class QuotaConfigParser {

  private static final Logger log = LoggerFactory.getLogger(QuotaConfigParser.class);

  private final ObjectMapper objectMapper;

  public QuotaConfigParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public QuotaConfig parse(String json) {
    if (json == null || json.isBlank()) {
      return QuotaConfig.empty();
    }
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode dims = root.path("dimensions");
      if (!dims.isObject()) {
        return QuotaConfig.empty();
      }
      Map<String, QuotaConfig.DimensionLimit> map = new HashMap<>();
      Iterator<Map.Entry<String, JsonNode>> fields = dims.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        JsonNode lim = entry.getValue();
        BigDecimal soft = decimalOrNull(lim.get("soft"));
        BigDecimal hard = decimalOrNull(lim.get("hard"));
        if (soft != null || hard != null) {
          map.put(entry.getKey(), new QuotaConfig.DimensionLimit(soft, hard));
        }
      }
      return new QuotaConfig(map);
    } catch (Exception ex) {
      log.warn("invalid quota_config JSON; treating as empty: {}", ex.getMessage());
      return QuotaConfig.empty();
    }
  }

  private static BigDecimal decimalOrNull(JsonNode node) {
    if (node == null || node.isNull() || !node.isNumber()) {
      return null;
    }
    return node.decimalValue();
  }
}
