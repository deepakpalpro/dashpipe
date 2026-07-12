package com.dashflow.api.k8s;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PipeletK8sProperties.class)
public class KubernetesClientConfig {

  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(prefix = "pipeline.k8s", name = "enabled", havingValue = "true")
  public KubernetesClient kubernetesClient() {
    // Uses ~/.kube/config (Rancher Desktop context) or in-cluster config when present.
    return new KubernetesClientBuilder().build();
  }
}
