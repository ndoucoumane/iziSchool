package com.izischool;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.izischool")
@EnableRedisRepositories(basePackages = {})
@EnableCaching
@EnableScheduling
public class IziSchoolApplication {

    @Value("${izischool.default-timezone:UTC}")
    private String defaultTimezone;

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(defaultTimezone));
    }

    public static void main(String[] args) {
        SpringApplication.run(IziSchoolApplication.class, args);
    }
}
