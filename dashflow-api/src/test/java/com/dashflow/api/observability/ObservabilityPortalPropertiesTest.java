package com.dashflow.api.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.dashflow.api.config.ObservabilityPortalProperties;
import org.junit.jupiter.api.Test;

class ObservabilityPortalPropertiesTest {

  @Test
  void blankUrlsDisableLinks() {
    ObservabilityPortalProperties props = new ObservabilityPortalProperties();
    assertThat(props.isGrafanaEnabled()).isFalse();
    assertThat(props.isElasticsearchEnabled()).isFalse();
    assertThat(props.normalizedGrafanaUrl()).isNull();
  }

  @Test
  void configuredUrlsEnableLinksAndTrimSlash() {
    ObservabilityPortalProperties props = new ObservabilityPortalProperties();
    props.setGrafanaBaseUrl("http://grafana.example/");
    props.setElasticsearchBaseUrl(" http://es.example ");
    assertThat(props.isGrafanaEnabled()).isTrue();
    assertThat(props.normalizedGrafanaUrl()).isEqualTo("http://grafana.example");
    assertThat(props.normalizedElasticsearchUrl()).isEqualTo("http://es.example");
  }
}
