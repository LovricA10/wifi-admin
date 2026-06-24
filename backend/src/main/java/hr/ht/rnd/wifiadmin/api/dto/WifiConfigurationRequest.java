package hr.ht.rnd.wifiadmin.api.dto;

import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import hr.ht.rnd.wifiadmin.domain.WifiBand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WifiConfigurationRequest(
        @NotBlank @Size(max = 64) String cpeId,
        @NotNull WifiBand wifiBand,
        @NotBlank @Size(max = 32) String ssid,
        EncryptionType encryptionType,
        @Size(max = 128) String password
) {}
