package hr.ht.rnd.wifiadmin.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationRequest;
import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationResponse;
import hr.ht.rnd.wifiadmin.application.WifiParameterService;
import hr.ht.rnd.wifiadmin.application.exception.CpeNotFoundException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformFaultException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformTimeoutException;
import hr.ht.rnd.wifiadmin.application.exception.WifiValidationException;
import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import hr.ht.rnd.wifiadmin.domain.WifiBand;
import hr.ht.rnd.wifiadmin.infrastructure.config.CorsProperties;
import hr.ht.rnd.wifiadmin.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WifiParameterController.class)
@Import(SecurityConfig.class)
class WifiParameterControllerTest {

    @TestConfiguration
    static class TestCorsConfig {
        @Bean
        CorsProperties corsProperties() {
            return new CorsProperties(List.of("http://localhost:5173"));
        }
    }

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WifiParameterService wifiParameterService;

    @Autowired
    ObjectMapper objectMapper;

    private static final WifiConfigurationResponse RESPONSE = new WifiConfigurationResponse(
            "CPE_001", WifiBand.BAND_2_4_GHZ, "TestSSID", EncryptionType.WPA2_PSK, "secret");

    @Test
    void get_validCpeId_returns200WithJson() throws Exception {
        when(wifiParameterService.getWifiParameter("CPE_001")).thenReturn(RESPONSE);

        mockMvc.perform(get("/wifi-parameter/CPE_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpeId").value("CPE_001"))
                .andExpect(jsonPath("$.ssid").value("TestSSID"))
                .andExpect(jsonPath("$.wifiBand").value("BAND_2_4_GHZ"));
    }

    @Test
    void get_cpeIdTooLong_returns400WithValidationError() throws Exception {
        String longId = "a".repeat(65);

        mockMvc.perform(get("/wifi-parameter/" + longId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void get_cpeNotFound_returns404WithErrorBody() throws Exception {
        when(wifiParameterService.getWifiParameter("UNKNOWN"))
                .thenThrow(new CpeNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/wifi-parameter/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CPE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void get_platformTimeout_returns502() throws Exception {
        when(wifiParameterService.getWifiParameter("CPE_001"))
                .thenThrow(new PlatformTimeoutException("timed out", null));

        mockMvc.perform(get("/wifi-parameter/CPE_001"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PLATFORM_TIMEOUT"));
    }

    @Test
    void get_platformFault_returns502() throws Exception {
        when(wifiParameterService.getWifiParameter("CPE_001"))
                .thenThrow(new PlatformFaultException("SOAP error"));

        mockMvc.perform(get("/wifi-parameter/CPE_001"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PLATFORM_FAULT"));
    }

    @Test
    void put_validRequest_returns200WithJson() throws Exception {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_2_4_GHZ, "SSID", EncryptionType.WPA2_PSK, "pass");
        when(wifiParameterService.updateWifiParameter(any())).thenReturn(RESPONSE);

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpeId").value("CPE_001"));
    }

    @Test
    void put_missingCpeId_returns400() throws Exception {
        var invalid = new WifiConfigurationRequest(null, WifiBand.BAND_2_4_GHZ, "SSID", null, null);

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void put_missingWifiBand_returns400() throws Exception {
        var invalid = new WifiConfigurationRequest("CPE_001", null, "SSID", null, null);

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void put_invalidEnumValue_returns400WithValidationError() throws Exception {
        String body = "{\"cpeId\":\"CPE_001\",\"wifiBand\":\"INVALID_BAND\",\"ssid\":\"TestSSID\"}";

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void put_businessValidationFailure_returns400() throws Exception {
        var request = new WifiConfigurationRequest("CPE_001", WifiBand.BAND_2_4_GHZ, "SSID", EncryptionType.WPA2_PSK, null);
        when(wifiParameterService.updateWifiParameter(any()))
                .thenThrow(new WifiValidationException("Password is required when encryption type is WPA2_PSK"));

        mockMvc.perform(put("/wifi-parameter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Password is required when encryption type is WPA2_PSK"));
    }
}
