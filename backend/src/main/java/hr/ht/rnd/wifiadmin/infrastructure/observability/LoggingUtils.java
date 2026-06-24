package hr.ht.rnd.wifiadmin.infrastructure.observability;

import org.slf4j.Logger;

public final class LoggingUtils {

    private LoggingUtils() {}

    public static void logSuccess(Logger log, String operation, String cpeId, long startMs) {
        log.info("operation={} cpeId={} status=success durationMs={}", operation, cpeId, elapsed(startMs));
    }

    public static void logFailure(Logger log, String operation, String cpeId, long startMs,
                                   String errorCategory, String reason) {
        log.warn("operation={} cpeId={} status=failure errorCategory={} reason={} durationMs={}",
                operation, cpeId, errorCategory, reason, elapsed(startMs));
    }

    private static long elapsed(long startMs) {
        return System.currentTimeMillis() - startMs;
    }
}
