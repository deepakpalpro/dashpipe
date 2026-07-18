package com.dashflow.connector.storage;

import com.dashflow.connector.spi.ConnectionTestResult;
import com.dashflow.connector.spi.Connector;
import com.dashflow.connector.spi.ConnectorConfig;
import com.dashflow.connector.spi.ConnectorContext;
import com.dashflow.connector.spi.ConnectorRequest;
import com.dashflow.connector.spi.ConnectorResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Built-in storage connector (architecture §9.5). Put/get objects via S3 API against LocalStack in
 * Wave 1.
 */
@Component
public class StorageConnector implements Connector {

  public static final String TYPE = "storage";
  public static final String SPI_VERSION = "1.0";
  public static final String PROP_BUCKET = "bucket";
  public static final String PROP_CREATE_BUCKET = "createBucketIfMissing";
  public static final String META_KEY = "key";

  private ConnectorContext context;
  private ConnectorConfig config;
  private S3Client s3Client;
  private String bucket;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getSpiVersion() {
    return SPI_VERSION;
  }

  @Override
  public void configure(ConnectorContext context, ConnectorConfig config) {
    closeClient();
    this.context = context;
    this.config = config;
    if (config == null) {
      return;
    }
    this.bucket = stringProp(PROP_BUCKET, null);
    this.s3Client = LocalStackS3ClientFactory.create(config.properties(), config.secrets());
  }

  @Override
  public ConnectionTestResult testConnection() {
    if (config == null || s3Client == null) {
      return ConnectionTestResult.failed("Connector is not configured");
    }
    if (bucket == null || bucket.isBlank()) {
      return ConnectionTestResult.failed("Missing required property: bucket");
    }
    long started = System.nanoTime();
    try {
      ensureBucket();
      s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
      long latencyMs = (System.nanoTime() - started) / 1_000_000L;
      return ConnectionTestResult.ok(latencyMs, "Connection successful");
    } catch (Exception ex) {
      long latencyMs = (System.nanoTime() - started) / 1_000_000L;
      return new ConnectionTestResult(
          false, latencyMs, "Connection failed: " + ex.getClass().getSimpleName());
    }
  }

  @Override
  public ConnectorResponse read(ConnectorRequest request) {
    if (s3Client == null || bucket == null) {
      return ConnectorResponse.failure("Connector is not configured");
    }
    String key = objectKey(request);
    if (key == null || key.isBlank()) {
      return ConnectorResponse.failure("Missing object key (headers.key or recordId)");
    }
    try {
      byte[] payload =
          s3Client
              .getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build())
              .asByteArray();
      return new ConnectorResponse(true, 200, payload, Map.of(META_KEY, key), null);
    } catch (S3Exception ex) {
      return ConnectorResponse.failure("S3 get failed: " + ex.awsErrorDetails().errorMessage());
    } catch (Exception ex) {
      return ConnectorResponse.failure("S3 get failed: " + ex.getClass().getSimpleName());
    }
  }

  @Override
  public ConnectorResponse write(ConnectorRequest request) {
    if (s3Client == null || bucket == null) {
      return ConnectorResponse.failure("Connector is not configured");
    }
    String key = objectKey(request);
    if (key == null || key.isBlank()) {
      return ConnectorResponse.failure("Missing object key (headers.key or recordId)");
    }
    try {
      ensureBucket();
      s3Client.putObject(
          PutObjectRequest.builder().bucket(bucket).key(key).build(),
          RequestBody.fromBytes(request.payload() == null ? new byte[0] : request.payload()));
      return new ConnectorResponse(true, 200, new byte[0], Map.of(META_KEY, key), null);
    } catch (S3Exception ex) {
      return ConnectorResponse.failure("S3 put failed: " + ex.awsErrorDetails().errorMessage());
    } catch (Exception ex) {
      return ConnectorResponse.failure("S3 put failed: " + ex.getClass().getSimpleName());
    }
  }

  @Override
  public void close() {
    closeClient();
    this.context = null;
    this.config = null;
    this.bucket = null;
  }

  private void ensureBucket() {
    boolean createIfMissing = booleanProp(PROP_CREATE_BUCKET, true);
    try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
    } catch (NoSuchBucketException ex) {
      if (!createIfMissing) {
        throw ex;
      }
      s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    } catch (S3Exception ex) {
      // LocalStack may return 404 status without NoSuchBucketException mapping.
      if (ex.statusCode() == 404 && createIfMissing) {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        return;
      }
      throw ex;
    }
  }

  private static String objectKey(ConnectorRequest request) {
    if (request.headers() != null) {
      Object key = request.headers().get(META_KEY);
      if (key != null && !key.toString().isBlank()) {
        return key.toString();
      }
    }
    return request.recordId();
  }

  private String stringProp(String key, String defaultValue) {
    Object value = config.properties().get(key);
    if (value == null || value.toString().isBlank()) {
      return defaultValue;
    }
    return value.toString();
  }

  private boolean booleanProp(String key, boolean defaultValue) {
    Object value = config.properties().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    return Boolean.parseBoolean(value.toString());
  }

  private void closeClient() {
    if (s3Client != null) {
      s3Client.close();
      s3Client = null;
    }
  }
}
