package hr.ht.rnd.wifiadmin.infrastructure.soap;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hr.ht.rnd.wifiadmin.application.exception.CpeNotFoundException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformFaultException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformTimeoutException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformUnavailableException;
import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import hr.ht.rnd.wifiadmin.domain.WifiBand;
import hr.ht.rnd.wifiadmin.domain.WifiConfiguration;
import hr.ht.rnd.wifiadmin.infrastructure.config.SoapPlatformProperties;
import hr.ht.rnd.wifiadmin.infrastructure.soap.model.GetCpeIdRequest;
import hr.ht.rnd.wifiadmin.infrastructure.soap.model.GetCpeIdResponse;
import hr.ht.rnd.wifiadmin.infrastructure.soap.model.SoapWifiConfiguration;
import hr.ht.rnd.wifiadmin.infrastructure.soap.model.UpdateCpeIdRequest;
import hr.ht.rnd.wifiadmin.infrastructure.soap.model.UpdateCpeIdResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SoapWifiPlatformClientTest {

    @RegisterExtension
    WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    private SoapWifiPlatformClient client;

    private static final String GET_RESPONSE_XML = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <ns:GetCpeIdResponse xmlns:ns="http://wifi-admin.local/platform/v1">
                  <ns:configuration>
                    <ns:cpeId>CPE_001</ns:cpeId>
                    <ns:wifiBand>BAND_2_4_GHZ</ns:wifiBand>
                    <ns:ssid>TestSSID</ns:ssid>
                    <ns:encryptionType>WPA2_PSK</ns:encryptionType>
                    <ns:password>secret</ns:password>
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
                    <ns:ssid>UpdatedSSID</ns:ssid>
                    <ns:encryptionType>WPA3_SAE</ns:encryptionType>
                    <ns:password>newpass</ns:password>
                  </ns:configuration>
                </ns:UpdateCpeIdResponse>
              </soap:Body>
            </soap:Envelope>
            """;

    private static final String FAULT_NOT_FOUND_XML = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <soap:Fault>
                  <faultcode>soap:Server</faultcode>
                  <faultstring>CPE not found</faultstring>
                </soap:Fault>
              </soap:Body>
            </soap:Envelope>
            """;

    private static final String FAULT_GENERIC_XML = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <soap:Fault>
                  <faultcode>soap:Server</faultcode>
                  <faultstring>Internal platform error</faultstring>
                </soap:Fault>
              </soap:Body>
            </soap:Envelope>
            """;

    @BeforeEach
    void setUp() throws Exception {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(
                GetCpeIdRequest.class, GetCpeIdResponse.class,
                UpdateCpeIdRequest.class, UpdateCpeIdResponse.class,
                SoapWifiConfiguration.class);
        marshaller.afterPropertiesSet();

        String platformUrl = wireMock.baseUrl() + "/platform";
        SoapPlatformProperties properties = new SoapPlatformProperties(platformUrl, 5000, 5000);

        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
        sender.setConnectionTimeout(Duration.ofMillis(5000));
        sender.setReadTimeout(Duration.ofMillis(5000));

        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        template.setMessageSender(sender);
        template.setDefaultUri(platformUrl);

        client = new SoapWifiPlatformClient(template, properties);
    }

    @Test
    void getByCpeId_sendsCorrectSoapActionHeader() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .withHeader("SOAPAction", containing("getCpeID"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/xml;charset=utf-8")
                        .withBody(GET_RESPONSE_XML)));

        client.getByCpeId("CPE_001");

        wireMock.verify(postRequestedFor(urlEqualTo("/platform"))
                .withHeader("SOAPAction", containing("getCpeID")));
    }

    @Test
    void getByCpeId_requestContainsNamespaceAndCpeId() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/xml;charset=utf-8")
                        .withBody(GET_RESPONSE_XML)));

        client.getByCpeId("CPE_001");

        wireMock.verify(postRequestedFor(urlEqualTo("/platform"))
                .withRequestBody(containing("http://wifi-admin.local/platform/v1"))
                .withRequestBody(containing("CPE_001")));
    }

    @Test
    void getByCpeId_parsesResponseCorrectly() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/xml;charset=utf-8")
                        .withBody(GET_RESPONSE_XML)));

        WifiConfiguration result = client.getByCpeId("CPE_001");

        assertThat(result.cpeId()).isEqualTo("CPE_001");
        assertThat(result.wifiBand()).isEqualTo(WifiBand.BAND_2_4_GHZ);
        assertThat(result.ssid()).isEqualTo("TestSSID");
        assertThat(result.encryptionType()).isEqualTo(EncryptionType.WPA2_PSK);
        assertThat(result.password()).isEqualTo("secret");
    }

    @Test
    void getByCpeId_soapFaultNotFound_throwsCpeNotFoundException() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/xml;charset=utf-8")
                        .withBody(FAULT_NOT_FOUND_XML)));

        assertThatThrownBy(() -> client.getByCpeId("CPE_001"))
                .isInstanceOf(CpeNotFoundException.class);
    }

    @Test
    void getByCpeId_genericSoapFault_throwsPlatformFaultException() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/xml;charset=utf-8")
                        .withBody(FAULT_GENERIC_XML)));

        assertThatThrownBy(() -> client.getByCpeId("CPE_001"))
                .isInstanceOf(PlatformFaultException.class);
    }

    @Test
    void getByCpeId_connectionRefused_throwsPlatformUnavailableException() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> client.getByCpeId("CPE_001"))
                .isInstanceOf(PlatformUnavailableException.class);
    }

    @Test
    void update_sendsCorrectSoapActionHeader() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .withHeader("SOAPAction", containing("updateCpeId"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/xml;charset=utf-8")
                        .withBody(UPDATE_RESPONSE_XML)));

        var config = new WifiConfiguration("CPE_001", WifiBand.BAND_5_GHZ, "UpdatedSSID", EncryptionType.WPA3_SAE, "newpass");
        client.update(config);

        wireMock.verify(postRequestedFor(urlEqualTo("/platform"))
                .withHeader("SOAPAction", containing("updateCpeId")));
    }

    @Test
    void update_parsesResponseCorrectly() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/xml;charset=utf-8")
                        .withBody(UPDATE_RESPONSE_XML)));

        var config = new WifiConfiguration("CPE_001", WifiBand.BAND_5_GHZ, "UpdatedSSID", EncryptionType.WPA3_SAE, "newpass");
        WifiConfiguration result = client.update(config);

        assertThat(result.cpeId()).isEqualTo("CPE_001");
        assertThat(result.wifiBand()).isEqualTo(WifiBand.BAND_5_GHZ);
        assertThat(result.ssid()).isEqualTo("UpdatedSSID");
        assertThat(result.encryptionType()).isEqualTo(EncryptionType.WPA3_SAE);
    }

    @Test
    void getByCpeId_readTimeout_throwsPlatformTimeoutException() {
        wireMock.stubFor(post(urlEqualTo("/platform"))
                .willReturn(aResponse()
                        .withFixedDelay(3000)
                        .withHeader("Content-Type", "text/xml;charset=utf-8")
                        .withBody(GET_RESPONSE_XML)));

        SoapPlatformProperties shortTimeout = new SoapPlatformProperties(wireMock.baseUrl() + "/platform", 1000, 500);

        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
        sender.setConnectionTimeout(Duration.ofMillis(1000));
        sender.setReadTimeout(Duration.ofMillis(500));

        try {
            Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
            marshaller.setClassesToBeBound(
                    GetCpeIdRequest.class, GetCpeIdResponse.class,
                    UpdateCpeIdRequest.class, UpdateCpeIdResponse.class,
                    SoapWifiConfiguration.class);
            marshaller.afterPropertiesSet();

            WebServiceTemplate template = new WebServiceTemplate();
            template.setMarshaller(marshaller);
            template.setUnmarshaller(marshaller);
            template.setMessageSender(sender);
            template.setDefaultUri(shortTimeout.url());

            SoapWifiPlatformClient shortTimeoutClient = new SoapWifiPlatformClient(template, shortTimeout);

            assertThatThrownBy(() -> shortTimeoutClient.getByCpeId("CPE_001"))
                    .isInstanceOf(PlatformTimeoutException.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
