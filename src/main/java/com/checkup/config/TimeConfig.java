package com.checkup.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "What time is it?" comes from this Clock instead of LocalDateTime.now() scattered
 * through the code. In tests we swap in Clock.fixed(...), so rules like "cancel at least
 * 24 hours ahead" can be tested at an exact moment instead of depending on today's date.
 */
@Configuration
public class TimeConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
