package hr.ht.rnd.wifiadmin.application;

import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationRequest;
import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationResponse;
import hr.ht.rnd.wifiadmin.application.exception.CpeNotFoundException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformTimeoutException;
import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import hr.ht.rnd.wifiadmin.domain.WifiBand;
import hr.ht.rnd.wifiadmin.domain.WifiConfiguration;
import hr.ht.rnd.wifiadmin.infrastructure.persistence.WifiConfigurationPersistenceAdapter;
import hr.ht.rnd.wifiadmin.infrastructure.soap.WifiPlatformClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WifiParameterServiceTest {

    @Mock
    WifiConfigurationValidator validator;

    @Mock
    WifiPlatformClient platformClient;

    @Mock
    WifiConfigurationPersistenceAdapter persistenceAdapter;

    @InjectMocks
    WifiParameterService service;

    private static final WifiConfiguration CONFIG = new WifiConfiguration(
            "CPE_001", WifiBand.BAND_2_4_GHZ, "TestSSID", EncryptionType.WPA2_PSK, "pass");

    @Test
    void getWifiParameter_foundInDb_returnsFromDbWithoutCallingPlatform() {
        when(persistenceAdapter.findByCpeId("CPE_001")).thenReturn(Optional.of(CONFIG));

        WifiConfigurationResponse response = service.getWifiParameter("CPE_001");

        assertThat(response.cpeId()).isEqualTo("CPE_001");
        assertThat(response.ssid()).isEqualTo("TestSSID");
        verifyNoInteractions(platformClient);
    }

    @Test
    void getWifiParameter_notInDb_fetchesFromPlatformAndSaves() {
        when(persistenceAdapter.findByCpeId("CPE_001")).thenReturn(Optional.empty());
        when(platformClient.getByCpeId("CPE_001")).thenReturn(CONFIG);
        when(persistenceAdapter.save(CONFIG)).thenReturn(CONFIG);

        WifiConfigurationResponse response = service.getWifiParameter("CPE_001");

        assertThat(response.cpeId()).isEqualTo("CPE_001");
        verify(platformClient).getByCpeId("CPE_001");
        verify(persistenceAdapter).save(CONFIG);
    }

    @Test
    void getWifiParameter_trimsCpeId() {
        when(persistenceAdapter.findByCpeId("CPE_001")).thenReturn(Optional.of(CONFIG));

        service.getWifiParameter("  CPE_001  ");

        verify(persistenceAdapter).findByCpeId("CPE_001");
    }

    @Test
    void getWifiParameter_platformThrowsCpeNotFound_propagatesException() {
        when(persistenceAdapter.findByCpeId("UNKNOWN")).thenReturn(Optional.empty());
        when(platformClient.getByCpeId("UNKNOWN")).thenThrow(new CpeNotFoundException("UNKNOWN"));

        assertThatThrownBy(() -> service.getWifiParameter("UNKNOWN"))
                .isInstanceOf(CpeNotFoundException.class);
    }

    @Test
    void updateWifiParameter_validatesCallsPlatformAndSaves() {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_2_4_GHZ, "NewSSID", EncryptionType.WPA2_PSK, "newpass");
        when(platformClient.update(any())).thenReturn(CONFIG);
        when(persistenceAdapter.save(any())).thenReturn(CONFIG);

        WifiConfigurationResponse response = service.updateWifiParameter(request);

        assertThat(response.cpeId()).isEqualTo("CPE_001");
        verify(validator).validate(any());
        verify(platformClient).update(any());
        verify(persistenceAdapter).save(any());
    }

    @Test
    void updateWifiParameter_nullEncryptionType_defaultsToOpen() {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_2_4_GHZ, "SSID", null, null);
        var expectedConfig = new WifiConfiguration("CPE_001", WifiBand.BAND_2_4_GHZ, "SSID", EncryptionType.OPEN, null);
        when(platformClient.update(expectedConfig)).thenReturn(expectedConfig);
        when(persistenceAdapter.save(expectedConfig)).thenReturn(expectedConfig);

        WifiConfigurationResponse response = service.updateWifiParameter(request);

        assertThat(response.encryptionType()).isEqualTo(EncryptionType.OPEN);
        verify(platformClient).update(expectedConfig);
    }

    @Test
    void updateWifiParameter_platformTimeout_propagatesException() {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_2_4_GHZ, "SSID", EncryptionType.OPEN, null);
        when(platformClient.update(any())).thenThrow(new PlatformTimeoutException("timed out", null));

        assertThatThrownBy(() -> service.updateWifiParameter(request))
                .isInstanceOf(PlatformTimeoutException.class);
    }

    @Test
    void refreshWifiParameter_fetchesFromPlatformAndCallsSaveFromSync() {
        when(platformClient.getByCpeId("CPE_001")).thenReturn(CONFIG);
        when(persistenceAdapter.saveFromSync(CONFIG)).thenReturn(CONFIG);

        WifiConfigurationResponse response = service.refreshWifiParameter("CPE_001");

        assertThat(response.cpeId()).isEqualTo("CPE_001");
        verify(persistenceAdapter).saveFromSync(CONFIG);
        verify(persistenceAdapter, never()).save(any());
    }
}
