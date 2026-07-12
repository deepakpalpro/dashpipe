package com.dashflow.connector.localstack;

import java.net.URI;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** Shared LocalStack endpoint / credential resolution for S3 and SQS connectors. */
public final class LocalStackAwsClientFactory {

  public static final String DEFAULT_ENDPOINT = "http://localhost:4567";
  public static final String DEFAULT_REGION = "us-east-1";
  public static final String DEFAULT_ACCESS_KEY = "test";
  public static final String DEFAULT_SECRET_KEY = "test";

  private LocalStackAwsClientFactory() {}

  public static String endpoint(Map<String, Object> properties) {
    return stringProp(properties, "endpoint", envOr("LOCALSTACK_ENDPOINT", DEFAULT_ENDPOINT));
  }

  public static String region(Map<String, Object> properties) {
    return stringProp(properties, "region", DEFAULT_REGION);
  }

  public static StaticCredentialsProvider credentials(
      Map<String, Object> properties, Map<String, String> secrets) {
    String accessKey =
        firstNonBlank(
            secrets == null ? null : secrets.get("accessKeyId"),
            stringProp(properties, "accessKeyId", null),
            DEFAULT_ACCESS_KEY);
    String secretKey =
        firstNonBlank(
            secrets == null ? null : secrets.get("secretAccessKey"),
            stringProp(properties, "secretAccessKey", null),
            DEFAULT_SECRET_KEY);
    return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
  }

  public static Region awsRegion(Map<String, Object> properties) {
    return Region.of(region(properties));
  }

  /**
   * LocalStack often returns queue URLs with host {@code *.localhost.localstack.cloud:4566}. Rewrite
   * to the configured host endpoint so the SDK hits the mapped port (default 4567).
   */
  public static String rewriteLocalStackUrl(String url, String endpoint) {
    if (url == null || url.isBlank() || endpoint == null || endpoint.isBlank()) {
      return url;
    }
    URI original = URI.create(url);
    URI ep = URI.create(endpoint);
    String path = original.getRawPath() == null ? "" : original.getRawPath();
    String query = original.getRawQuery() == null ? "" : "?" + original.getRawQuery();
    return ep.getScheme()
        + "://"
        + ep.getAuthority()
        + path
        + query;
  }

  public static String stringProp(Map<String, Object> properties, String key, String defaultValue) {
    Object value = properties == null ? null : properties.get(key);
    if (value == null || value.toString().isBlank()) {
      return defaultValue;
    }
    return value.toString();
  }

  private static String envOr(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? defaultValue : value;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }
}
