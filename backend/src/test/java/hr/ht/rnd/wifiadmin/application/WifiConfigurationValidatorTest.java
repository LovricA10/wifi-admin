package hr.ht.rnd.wifiadmin.application;

import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationRequest;
import hr.ht.rnd.wifiadmin.application.exception.WifiValidationException;
import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import hr.ht.rnd.wifiadmin.domain.WifiBand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WifiConfigurationValidatorTest {

    private final WifiConfigurationValidator validator = new WifiConfigurationValidator();

    @Test
    void open_withoutPassword_isValid() {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_2_4_GHZ, "SSID", EncryptionType.OPEN, null);
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void nullEncryption_treatedAsOpen_isValid() {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_2_4_GHZ, "SSID", null, null);
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void wpa2Psk_withPassword_isValid() {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_2_4_GHZ, "SSID", EncryptionType.WPA2_PSK, "secret");
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void wpa2Psk_withoutPassword_throwsValidationException() {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_2_4_GHZ, "SSID", EncryptionType.WPA2_PSK, null);
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(WifiValidationException.class)
                .hasMessageContaining("WPA2_PSK");
    }

    @Test
    void wpa2Psk_withBlankPassword_throwsValidationException() {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_2_4_GHZ, "SSID", EncryptionType.WPA2_PSK, "   ");
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(WifiValidationException.class);
    }

    @Test
    void wep_withoutPassword_throwsValidationException() {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_2_4_GHZ, "SSID", EncryptionType.WEP, null);
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(WifiValidationException.class)
                .hasMessageContaining("WEP");
    }

    @Test
    void wpa3Sae_withPassword_isValid() {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_5_GHZ, "SSID", EncryptionType.WPA3_SAE, "pass");
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }
}
