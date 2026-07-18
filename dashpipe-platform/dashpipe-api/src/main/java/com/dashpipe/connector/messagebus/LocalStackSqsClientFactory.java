package com.dashpipe.connector.messagebus;

import com.dashpipe.connector.localstack.LocalStackAwsClientFactory;
import java.net.URI;
import java.util.Map;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Builds an SQS client pointed at LocalStack. */
public final class LocalStackSqsClientFactory {

  private LocalStackSqsClientFactory() {}

  public static SqsClient create(Map<String, Object> properties, Map<String, String> secrets) {
    return SqsClient.builder()
        .endpointOverride(URI.create(LocalStackAwsClientFactory.endpoint(properties)))
        .region(LocalStackAwsClientFactory.awsRegion(properties))
        .credentialsProvider(LocalStackAwsClientFactory.credentials(properties, secrets))
        .build();
  }
}
