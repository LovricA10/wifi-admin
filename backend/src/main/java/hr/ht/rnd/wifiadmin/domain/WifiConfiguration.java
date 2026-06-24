package hr.ht.rnd.wifiadmin.domain;

public record WifiConfiguration(
        String cpeId,
        WifiBand wifiBand,
        String ssid,
        EncryptionType encryptionType,
        String password
) {}
