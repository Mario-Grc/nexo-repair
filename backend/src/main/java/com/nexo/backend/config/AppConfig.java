package com.nexo.backend.config;

import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Workshop time zone used to interpret date filters.
@Configuration
public class AppConfig {

    @Bean
    public ZoneId workshopZone(@Value("${nexo.timezone}") String id) {
        return ZoneId.of(id);
    }
}
