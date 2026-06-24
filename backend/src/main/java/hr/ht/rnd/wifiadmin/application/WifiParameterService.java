package hr.ht.rnd.wifiadmin.application;

import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationRequest;
import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationResponse;
import org.springframework.stereotype.Service;

@Service
public class WifiParameterService {

    private final WifiConfigurationValidator validator;

    WifiParameterService(WifiConfigurationValidator validator) {
        this.validator = validator;
    }

    public WifiConfigurationResponse getWifiParameter(String cpeId) {
        throw new UnsupportedOperationException("Implemented in Phase 4");
    }

    public WifiConfigurationResponse updateWifiParameter(WifiConfigurationRequest request) {
        validator.validate(request);
        throw new UnsupportedOperationException("Implemented in Phase 4");
    }
}
