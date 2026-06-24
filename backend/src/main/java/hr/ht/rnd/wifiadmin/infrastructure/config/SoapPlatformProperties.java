package hr.ht.rnd.wifiadmin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wifi-admin.platform.soap")
public record SoapPlatformProperties(
        String url,
        int connectTimeoutMs,
        int readTimeoutMs
) {}
