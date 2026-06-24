package hr.ht.rnd.wifiadmin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "wifi-admin.cors")
public record CorsProperties(List<String> allowedOrigins) {}
