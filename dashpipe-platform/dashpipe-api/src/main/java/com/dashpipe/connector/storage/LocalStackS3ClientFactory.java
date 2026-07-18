package com.dashpipe.connector.storage;

import com.dashpipe.connector.localstack.LocalStackAwsClientFactory;
import java.net.URI;
import java.util.Map;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/** Builds an S3 client pointed at LocalStack (path-style). */
public final class LocalStackS3ClientFactory {

  public static final String DEFAULT_ENDPOINT = LocalStackAwsClientFactory.DEFAULT_ENDPOINT;
  public static final String DEFAULT_REGION = LocalStackAwsClientFactory.DEFAULT_REGION;

  private LocalStackS3ClientFactory() {}

  public static S3Client create(Map<String, Object> properties, Map<String, String> secrets) {
    return S3Client.builder()
        .endpointOverride(URI.create(LocalStackAwsClientFactory.endpoint(properties)))
        .region(LocalStackAwsClientFactory.awsRegion(properties))
        .credentialsProvider(LocalStackAwsClientFactory.credentials(properties, secrets))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }
}
