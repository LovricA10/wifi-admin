package hr.ht.rnd.wifiadmin.application;

import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationRequest;
import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationResponse;
import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import hr.ht.rnd.wifiadmin.domain.WifiConfiguration;
import hr.ht.rnd.wifiadmin.infrastructure.soap.WifiPlatformClient;
import org.springframework.stereotype.Service;

@Service
public class WifiParameterService {

    private final WifiConfigurationValidator validator;
    private final WifiPlatformClient platformClient;

    WifiParameterService(WifiConfigurationValidator validator, WifiPlatformClient platformClient) {
        this.validator = validator;
        this.platformClient = platformClient;
    }

    public WifiConfigurationResponse getWifiParameter(String cpeId) {
        WifiConfiguration configuration = platformClient.getByCpeId(cpeId.trim());
        return toResponse(configuration);
    }

    public WifiConfigurationResponse updateWifiParameter(WifiConfigurationRequest request) {
        WifiConfigurationRequest trimmed = trim(request);
        validator.validate(trimmed);
        WifiConfiguration configuration = toDomain(trimmed);
        WifiConfiguration updated = platformClient.update(configuration);
        return toResponse(updated);
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
