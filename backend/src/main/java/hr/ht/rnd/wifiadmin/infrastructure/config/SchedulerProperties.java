package hr.ht.rnd.wifiadmin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "wifi-admin.sync")
public record SchedulerProperties(
        boolean enabled,
        String cron,
        List<String> cpeIds
) {}
