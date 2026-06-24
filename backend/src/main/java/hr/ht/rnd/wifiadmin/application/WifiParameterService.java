package hr.ht.rnd.wifiadmin.application;

import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationRequest;
import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationResponse;
import hr.ht.rnd.wifiadmin.application.exception.CpeNotFoundException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformFaultException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformTimeoutException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformUnavailableException;
import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import hr.ht.rnd.wifiadmin.domain.WifiConfiguration;
import hr.ht.rnd.wifiadmin.infrastructure.observability.LoggingUtils;
import hr.ht.rnd.wifiadmin.infrastructure.persistence.WifiConfigurationPersistenceAdapter;
import hr.ht.rnd.wifiadmin.infrastructure.soap.WifiPlatformClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WifiParameterService {

    private static final Logger log = LoggerFactory.getLogger(WifiParameterService.class);

    private final WifiConfigurationValidator validator;
    private final WifiPlatformClient platformClient;
    private final WifiConfigurationPersistenceAdapter persistenceAdapter;

    WifiParameterService(
            WifiConfigurationValidator validator,
            WifiPlatformClient platformClient,
            WifiConfigurationPersistenceAdapter persistenceAdapter
    ) {
        this.validator = validator;
        this.platformClient = platformClient;
        this.persistenceAdapter = persistenceAdapter;
    }

    public WifiConfigurationResponse getWifiParameter(String cpeId) {
        String normalizedCpeId = cpeId.trim();
        long startMs = System.currentTimeMillis();
        try {
            WifiConfigurationResponse response = persistenceAdapter.findByCpeId(normalizedCpeId)
                    .map(this::toResponse)
                    .orElseGet(() -> {
                        WifiConfiguration fromPlatform = platformClient.getByCpeId(normalizedCpeId);
                        WifiConfiguration saved = persistenceAdapter.save(fromPlatform);
                        return toResponse(saved);
                    });
            LoggingUtils.logSuccess(log, "getWifiParameter", normalizedCpeId, startMs);
            return response;
        } catch (RuntimeException ex) {
            LoggingUtils.logFailure(log, "getWifiParameter", normalizedCpeId, startMs, errorCategory(ex), ex.getMessage());
            throw ex;
        }
    }

    public WifiConfigurationResponse updateWifiParameter(WifiConfigurationRequest request) {
        WifiConfigurationRequest trimmed = trim(request);
        validator.validate(trimmed);
        String cpeId = trimmed.cpeId();
        long startMs = System.currentTimeMillis();
        try {
            WifiConfiguration configuration = toDomain(trimmed);
            WifiConfiguration confirmed = platformClient.update(configuration);
            WifiConfiguration saved = persistenceAdapter.save(confirmed);
            WifiConfigurationResponse response = toResponse(saved);
            LoggingUtils.logSuccess(log, "updateWifiParameter", cpeId, startMs);
            return response;
        } catch (RuntimeException ex) {
            LoggingUtils.logFailure(log, "updateWifiParameter", cpeId, startMs, errorCategory(ex), ex.getMessage());
            throw ex;
        }
    }

    public WifiConfigurationResponse refreshWifiParameter(String cpeId) {
        String normalizedCpeId = cpeId.trim();
        long startMs = System.currentTimeMillis();
        try {
            WifiConfiguration fromPlatform = platformClient.getByCpeId(normalizedCpeId);
            WifiConfiguration saved = persistenceAdapter.saveFromSync(fromPlatform);
            WifiConfigurationResponse response = toResponse(saved);
            LoggingUtils.logSuccess(log, "refreshWifiParameter", normalizedCpeId, startMs);
            return response;
        } catch (RuntimeException ex) {
            LoggingUtils.logFailure(log, "refreshWifiParameter", normalizedCpeId, startMs, errorCategory(ex), ex.getMessage());
            throw ex;
        }
    }

    private String errorCategory(RuntimeException ex) {
        if (ex instanceof CpeNotFoundException) return "CPE_NOT_FOUND";
        if (ex instanceof PlatformTimeoutException) return "PLATFORM_TIMEOUT";
        if (ex instanceof PlatformUnavailableException) return "PLATFORM_UNAVAILABLE";
        if (ex instanceof PlatformFaultException) return "PLATFORM_FAULT";
        return "INTERNAL_ERROR";
    }

    private WifiConfigurationRequest trim(WifiConfigurationRequest request) {
        return new WifiConfigurationRequest(
                trimOrNull(request.cpeId()),
                request.wifiBand(),
                trimOrNull(request.ssid()),
                request.encryptionType(),
                trimOrNull(request.password())
        );
    }

    private WifiConfiguration toDomain(WifiConfigurationRequest request) {
        EncryptionType encryptionType = request.encryptionType() != null
                ? request.encryptionType()
                : EncryptionType.OPEN;
        return new WifiConfiguration(
                request.cpeId(),
                request.wifiBand(),
                request.ssid(),
                encryptionType,
                request.password()
        );
    }

    private WifiConfigurationResponse toResponse(WifiConfiguration configuration) {
        return new WifiConfigurationResponse(
                configuration.cpeId(),
                configuration.wifiBand(),
                configuration.ssid(),
                configuration.encryptionType(),
                configuration.password()
        );
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
