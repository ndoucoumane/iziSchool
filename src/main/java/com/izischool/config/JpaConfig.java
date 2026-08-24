package com.izischool.config;

import com.izischool.tenant.context.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            String userId = TenantContext.getCurrentKeycloakUserId();
            return Optional.ofNullable(userId != null ? userId : "SYSTEM");
        };
    }
}
