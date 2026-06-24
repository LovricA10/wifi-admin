package hr.ht.rnd.wifiadmin;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationRequest;
import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationResponse;
import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import hr.ht.rnd.wifiadmin.domain.WifiBand;
import hr.ht.rnd.wifiadmin.domain.WifiConfiguration;
import hr.ht.rnd.wifiadmin.infrastructure.persistence.WifiConfigurationPersistenceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WifiParameterIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configurePlatformUrl(DynamicPropertyRegistry registry) {
        registry.add("wifi-admin.platform.soap.url", () -> wireMock.baseUrl() + "/platform");
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    WifiConfigurationPersistenceAdapter persistenceAdapter;

    private static final String GET_RESPONSE_XML = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <ns:GetCpeIdResponse xmlns:ns="http://wifi-admin.local/platform/v1">
                  <ns:configuration>
                    <ns:cpeId>CPE_001</ns:cpeId>
                    <ns:wifiBand>BAND_2_4_GHZ</ns:wifiBand>
                    <ns:ssid>SoapSSID</ns:ssid>
                    <ns:encryptionType>WPA2_PSK</ns:encryptionType>
                    <ns:password>soappass</ns:password>
                  </ns:configuration>
                </ns:GetCpeIdResponse>
              </soap:Body>
            </soap:Envelope>
            """;

    private static final String UPDATE_RESPONSE_XML = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <ns:UpdateCpeIdResponse xmlns:ns="http://wifi-admin.local/platform/v1">
                  <ns:configuration>
                    <ns:cpeId>CPE_001</ns:cpeId>
                    <ns:wifiBand>BAND_5_GHZ</ns:wifiBand>
                    <ns:ssid>NewSSID</ns:ssid>
                    <ns:encryptionType>WPA2_PSK</ns:encryptionType>
                    <ns:password>newpass</ns:password>
                  </ns:configuration>
                </ns:UpdateCpeIdResponse>
              </soap:Body>
            </soap:Envelope>
            """;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        jdbcTemplate.execute("DELETE FROM wifi_configuration");
    }

    @Test
    void get_returnsFromDb_whenRecordExists() {
        persistenceAdapter.save(new WifiConfiguration(
                "CPE_001", WifiBand.BAND_2_4_GHZ, "DbSSID", EncryptionType.WPA2_PSK, null));

        ResponseEntity<WifiConfigurationResponse> response =
                restTemplate.getForEntity("/wifi-parameter/CPE_001", WifiConfigurationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().ssid()).isEqualTo("DbSSID");
        wireMock.verify(0, postRequestedFor(urlEqualTo("/platform")));
    }

    @Test
    void get_fetchesFromSoapAndSavesToDb_whenNotInDb() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .withHeader("SOAPAction", containing("getCpeID"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/xml;charset=utf-8")
                        .withBody(GET_RESPONSE_XML)));

        ResponseEntity<WifiConfigurationResponse> response =
                restTemplate.getForEntity("/wifi-parameter/CPE_001", WifiConfigurationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().ssid()).isEqualTo("SoapSSID");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wifi_configuration WHERE cpe_id = 'CPE_001'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void put_callsPlatformAndSavesToDb() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .withHeader("SOAPAction", containing("updateCpeId"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/xml;charset=utf-8")
                        .withBody(UPDATE_RESPONSE_XML)));

        var request = new WifiConfigurationRequest(
                "CPE_001", WifiBand.BAND_5_GHZ, "NewSSID", EncryptionType.WPA2_PSK, "newpass");

        ResponseEntity<WifiConfigurationResponse> response = restTemplate.exchange(
                "/wifi-parameter", HttpMethod.PUT,
                new HttpEntity<>(request, jsonHeaders()),
                WifiConfigurationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().ssid()).isEqualTo("NewSSID");

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wifi_configuration", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void get_afterPut_returnsFromDbWithoutCallingSoap() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/xml;charset=utf-8")
                        .withBody(UPDATE_RESPONSE_XML)));

        var request = new WifiConfigurationRequest(
                "CPE_001", WifiBand.BAND_5_GHZ, "NewSSID", EncryptionType.WPA2_PSK, "newpass");
        restTemplate.exchange("/wifi-parameter", HttpMethod.PUT,
                new HttpEntity<>(request, jsonHeaders()), WifiConfigurationResponse.class);

        wireMock.resetAll();

        ResponseEntity<WifiConfigurationResponse> response =
                restTemplate.getForEntity("/wifi-parameter/CPE_001", WifiConfigurationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        wireMock.verify(0, postRequestedFor(urlEqualTo("/platform")));
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
