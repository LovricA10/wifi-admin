package hr.ht.rnd.wifiadmin.api.dto;

import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import hr.ht.rnd.wifiadmin.domain.WifiBand;

public record WifiConfigurationResponse(
        String cpeId,
        WifiBand wifiBand,
        String ssid,
        EncryptionType encryptionType,
        String password
) {}
