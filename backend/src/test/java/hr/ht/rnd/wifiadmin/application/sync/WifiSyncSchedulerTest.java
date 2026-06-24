package hr.ht.rnd.wifiadmin.application.sync;

import hr.ht.rnd.wifiadmin.application.WifiParameterService;
import hr.ht.rnd.wifiadmin.application.exception.PlatformUnavailableException;
import hr.ht.rnd.wifiadmin.infrastructure.config.SchedulerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WifiSyncSchedulerTest {

    @Mock
    WifiParameterService wifiParameterService;

    @Mock
    SchedulerProperties properties;

    @InjectMocks
    WifiSyncScheduler scheduler;

    @Test
    void sync_allCpesSucceed_refreshesAll() {
        when(properties.cpeIds()).thenReturn(List.of("CPE_001", "CPE_002"));

        scheduler.sync();

        verify(wifiParameterService).refreshWifiParameter("CPE_001");
        verify(wifiParameterService).refreshWifiParameter("CPE_002");
    }

    @Test
    void sync_partialFailure_continuesRemainingCpes() {
        when(properties.cpeIds()).thenReturn(List.of("CPE_001", "CPE_002", "CPE_003"));
        doThrow(new PlatformUnavailableException("unavailable", null))
                .when(wifiParameterService).refreshWifiParameter("CPE_002");

        scheduler.sync();

        verify(wifiParameterService).refreshWifiParameter("CPE_001");
        verify(wifiParameterService).refreshWifiParameter("CPE_002");
        verify(wifiParameterService).refreshWifiParameter("CPE_003");
    }

    @Test
    void sync_emptyCpeIds_skipsWithoutCallingService() {
        when(properties.cpeIds()).thenReturn(List.of());

        scheduler.sync();

        verifyNoInteractions(wifiParameterService);
    }

    @Test
    void sync_nullCpeIds_skipsWithoutCallingService() {
        when(properties.cpeIds()).thenReturn(null);

        scheduler.sync();

        verifyNoInteractions(wifiParameterService);
    }

    @Test
    void sync_allCpesFail_doesNotThrow() {
        when(properties.cpeIds()).thenReturn(List.of("CPE_001", "CPE_002"));
        doThrow(new PlatformUnavailableException("unavailable", null))
                .when(wifiParameterService).refreshWifiParameter(any());

        org.assertj.core.api.Assertions.assertThatCode(() -> scheduler.sync())
                .doesNotThrowAnyException();
    }
}
