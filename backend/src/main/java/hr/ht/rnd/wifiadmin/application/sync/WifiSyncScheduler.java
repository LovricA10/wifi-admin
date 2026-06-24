package hr.ht.rnd.wifiadmin.application.sync;

import hr.ht.rnd.wifiadmin.application.WifiParameterService;
import hr.ht.rnd.wifiadmin.infrastructure.config.SchedulerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "wifi-admin.sync.enabled", havingValue = "true")
public class WifiSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(WifiSyncScheduler.class);

    private final WifiParameterService wifiParameterService;
    private final SchedulerProperties properties;

    WifiSyncScheduler(WifiParameterService wifiParameterService, SchedulerProperties properties) {
        this.wifiParameterService = wifiParameterService;
        this.properties = properties;
    }

    @Scheduled(cron = "${wifi-admin.sync.cron}")
    public void sync() {
        String correlationId = "sync-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("correlationId", correlationId);
        try {
            runSync();
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void runSync() {
        List<String> cpeIds = properties.cpeIds() != null ? properties.cpeIds() : List.of();
        if (cpeIds.isEmpty()) {
            log.info("sync: no CPE IDs configured, skipping");
            return;
        }

        long startMs = System.currentTimeMillis();
        int total = cpeIds.size();
        List<String> failed = new ArrayList<>();

        for (String cpeId : cpeIds) {
            try {
                wifiParameterService.refreshWifiParameter(cpeId);
            } catch (Exception ex) {
                failed.add(cpeId);
                log.warn("sync: failed cpeId={} reason={}", cpeId, ex.getMessage());
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        int success = total - failed.size();

        if (failed.isEmpty()) {
            log.info("sync: total={} success={} failed=0 durationMs={}", total, success, durationMs);
        } else {
            log.warn("sync: total={} success={} failed={} failedIds={} durationMs={}",
                    total, success, failed.size(), failed, durationMs);
        }
    }
}
