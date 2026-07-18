package com.dashpipe.api.usage;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class UsageSchedulingConfig {

  @Bean
  @ConditionalOnMissingBean(Clock.class)
  Clock systemUtcClock() {
    return Clock.systemUTC();
  }
}
