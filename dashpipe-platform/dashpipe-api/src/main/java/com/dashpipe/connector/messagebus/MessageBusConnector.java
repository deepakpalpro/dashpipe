package com.dashpipe.connector.messagebus;

import com.dashpipe.connector.localstack.LocalStackAwsClientFactory;
import com.dashpipe.connector.spi.ConnectionTestResult;
import com.dashpipe.connector.spi.Connector;
import com.dashpipe.connector.spi.ConnectorConfig;
import com.dashpipe.connector.spi.ConnectorContext;
import com.dashpipe.connector.spi.ConnectorRequest;
import com.dashpipe.connector.spi.ConnectorResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

/**
 * Built-in message bus connector (architecture §9.5 {@code message_bus}). Publishes to LocalStack
 * SQS — not the platform's internal RabbitMQ topology (Wave 2).
 */
@Component
public class MessageBusConnector implements Connector {

  public static final String TYPE = "message_bus";
  public static final String SPI_VERSION = "1.0";
  public static final String PROP_QUEUE_NAME = "queueName";
  public static final String PROP_QUEUE_URL = "queueUrl";
  public static final String PROP_CREATE_QUEUE = "createQueueIfMissing";
  public static final String META_MESSAGE_ID = "messageId";

  private ConnectorContext context;
  private ConnectorConfig config;
  private SqsClient sqsClient;
  private String endpoint;
  private String queueUrl;

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
    this.endpoint = LocalStackAwsClientFactory.endpoint(config.properties());
    this.sqsClient = LocalStackSqsClientFactory.create(config.properties(), config.secrets());
    this.queueUrl = resolveQueueUrl();
  }

  @Override
  public ConnectionTestResult testConnection() {
    if (config == null || sqsClient == null) {
      return ConnectionTestResult.failed("Connector is not configured");
    }
    long started = System.nanoTime();
    try {
      ensureQueue();
      sqsClient.getQueueAttributes(
          GetQueueAttributesRequest.builder()
              .queueUrl(queueUrl)
              .attributeNames(QueueAttributeName.QUEUE_ARN)
              .build());
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
    if (sqsClient == null || queueUrl == null) {
      return ConnectorResponse.failure("Connector is not configured");
    }
    try {
      ensureQueue();
      var messages =
          sqsClient
              .receiveMessage(
                  ReceiveMessageRequest.builder()
                      .queueUrl(queueUrl)
                      .maxNumberOfMessages(1)
                      .waitTimeSeconds(1)
                      .build())
              .messages();
      if (messages.isEmpty()) {
        return new ConnectorResponse(true, 204, new byte[0], Map.of(), null);
      }
      var message = messages.getFirst();
      return new ConnectorResponse(
          true,
          200,
          message.body().getBytes(StandardCharsets.UTF_8),
          Map.of(META_MESSAGE_ID, message.messageId()),
          null);
    } catch (SqsException ex) {
      return ConnectorResponse.failure("SQS receive failed: " + ex.awsErrorDetails().errorMessage());
    } catch (Exception ex) {
      return ConnectorResponse.failure("SQS receive failed: " + ex.getClass().getSimpleName());
    }
  }

  @Override
  public ConnectorResponse write(ConnectorRequest request) {
    if (sqsClient == null) {
      return ConnectorResponse.failure("Connector is not configured");
    }
    try {
      ensureQueue();
      String body =
          request.payload() == null
              ? ""
              : new String(request.payload(), StandardCharsets.UTF_8);
      var response =
          sqsClient.sendMessage(
              SendMessageRequest.builder().queueUrl(queueUrl).messageBody(body).build());
      return new ConnectorResponse(
          true, 200, new byte[0], Map.of(META_MESSAGE_ID, response.messageId()), null);
    } catch (SqsException ex) {
      return ConnectorResponse.failure("SQS publish failed: " + ex.awsErrorDetails().errorMessage());
    } catch (Exception ex) {
      return ConnectorResponse.failure("SQS publish failed: " + ex.getClass().getSimpleName());
    }
  }

  @Override
  public void close() {
    closeClient();
    this.context = null;
    this.config = null;
    this.queueUrl = null;
    this.endpoint = null;
  }

  private String resolveQueueUrl() {
    String configuredUrl =
        LocalStackAwsClientFactory.stringProp(config.properties(), PROP_QUEUE_URL, null);
    if (configuredUrl != null) {
      return LocalStackAwsClientFactory.rewriteLocalStackUrl(configuredUrl, endpoint);
    }
    return null;
  }

  private void ensureQueue() {
    if (queueUrl != null && !queueUrl.isBlank()) {
      return;
    }
    String queueName =
        LocalStackAwsClientFactory.stringProp(config.properties(), PROP_QUEUE_NAME, null);
    if (queueName == null || queueName.isBlank()) {
      throw new IllegalStateException("Missing required property: queueName or queueUrl");
    }
    boolean createIfMissing = booleanProp(PROP_CREATE_QUEUE, true);
    try {
      String url =
          sqsClient
              .getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build())
              .queueUrl();
      queueUrl = LocalStackAwsClientFactory.rewriteLocalStackUrl(url, endpoint);
    } catch (QueueDoesNotExistException ex) {
      if (!createIfMissing) {
        throw ex;
      }
      String url =
          sqsClient
              .createQueue(CreateQueueRequest.builder().queueName(queueName).build())
              .queueUrl();
      queueUrl = LocalStackAwsClientFactory.rewriteLocalStackUrl(url, endpoint);
    } catch (SqsException ex) {
      if (createIfMissing
          && (ex.statusCode() == 400 || ex.statusCode() == 404)
          && ex.awsErrorDetails() != null
          && List.of("AWS.SimpleQueueService.NonExistentQueue", "QueueDoesNotExist")
              .contains(ex.awsErrorDetails().errorCode())) {
        String url =
            sqsClient
                .createQueue(CreateQueueRequest.builder().queueName(queueName).build())
                .queueUrl();
        queueUrl = LocalStackAwsClientFactory.rewriteLocalStackUrl(url, endpoint);
        return;
      }
      throw ex;
    }
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
    if (sqsClient != null) {
      sqsClient.close();
      sqsClient = null;
    }
  }
}
