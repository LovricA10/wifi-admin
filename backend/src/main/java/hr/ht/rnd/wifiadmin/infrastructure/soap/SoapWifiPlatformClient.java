package hr.ht.rnd.wifiadmin.infrastructure.soap;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import java.net.SocketTimeoutException;

@Component
public class SoapWifiPlatformClient implements WifiPlatformClient {

    private static final Logger log = LoggerFactory.getLogger(SoapWifiPlatformClient.class);

    private static final String NAMESPACE = "http://wifi-admin.local/platform/v1";
    private static final String ACTION_GET_CPE_ID = NAMESPACE + "#getCpeID";
    private static final String ACTION_UPDATE_CPE_ID = NAMESPACE + "#updateCpeId";

    private final WebServiceTemplate webServiceTemplate;
    private final SoapPlatformProperties properties;

    SoapWifiPlatformClient(WebServiceTemplate webServiceTemplate, SoapPlatformProperties properties) {
        this.webServiceTemplate = webServiceTemplate;
        this.properties = properties;
    }

    @Override
    public WifiConfiguration getByCpeId(String cpeId) {
        var request = new GetCpeIdRequest();
        request.setCpeId(cpeId);

        try {
            var response = (GetCpeIdResponse) webServiceTemplate.marshalSendAndReceive(
                    properties.url(), request, new SoapActionCallback(ACTION_GET_CPE_ID));
            return toDomain(response.getConfiguration(), cpeId);
        } catch (SoapFaultClientException ex) {
            throw mapSoapFault(ex, cpeId);
        } catch (WebServiceIOException ex) {
            throw mapIoException(ex);
        }
    }

    @Override
    public WifiConfiguration update(WifiConfiguration configuration) {
        var soapConfig = fromDomain(configuration);
        var request = new UpdateCpeIdRequest();
        request.setConfiguration(soapConfig);

        try {
            var response = (UpdateCpeIdResponse) webServiceTemplate.marshalSendAndReceive(
                    properties.url(), request, new SoapActionCallback(ACTION_UPDATE_CPE_ID));
            return toDomain(response.getConfiguration(), configuration.cpeId());
        } catch (SoapFaultClientException ex) {
            throw mapSoapFault(ex, configuration.cpeId());
        } catch (WebServiceIOException ex) {
            throw mapIoException(ex);
        }
    }

    private RuntimeException mapSoapFault(SoapFaultClientException ex, String cpeId) {
        String faultString = ex.getFaultStringOrReason();
        log.warn("SOAP fault: operation cpeId={} fault={}", cpeId, faultString);
        if (faultString != null && faultString.toLowerCase().contains("not found")) {
            return new CpeNotFoundException(cpeId);
        }
        return new PlatformFaultException("SOAP fault: " + faultString);
    }

    private RuntimeException mapIoException(WebServiceIOException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof SocketTimeoutException) {
            log.warn("SOAP platform request timed out: {}", ex.getMessage());
            return new PlatformTimeoutException("Platform request timed out", ex);
        }
        log.warn("SOAP platform I/O error: {}", ex.getMessage());
        return new PlatformUnavailableException("Platform unavailable: " + ex.getMessage(), ex);
    }

    private WifiConfiguration toDomain(SoapWifiConfiguration soap, String cpeId) {
        if (soap == null) {
            throw new PlatformFaultException("Platform returned empty configuration for cpeId=" + cpeId);
        }
        return new WifiConfiguration(
                soap.getCpeId(),
                parseWifiBand(soap.getWifiBand(), cpeId),
                soap.getSsid(),
                parseEncryptionType(soap.getEncryptionType()),
                soap.getPassword()
        );
    }

    private SoapWifiConfiguration fromDomain(WifiConfiguration config) {
        var soap = new SoapWifiConfiguration();
        soap.setCpeId(config.cpeId());
        soap.setWifiBand(config.wifiBand() != null ? config.wifiBand().name() : null);
        soap.setSsid(config.ssid());
        soap.setEncryptionType(config.encryptionType() != null ? config.encryptionType().name() : null);
        soap.setPassword(config.password());
        return soap;
    }

    private WifiBand parseWifiBand(String value, String cpeId) {
        if (value == null || value.isBlank()) {
            throw new PlatformFaultException("Platform returned null wifiBand for cpeId=" + cpeId);
        }
        try {
            return WifiBand.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new PlatformFaultException("Platform returned unknown wifiBand='" + value + "' for cpeId=" + cpeId);
        }
    }

    private EncryptionType parseEncryptionType(String value) {
        if (value == null || value.isBlank()) {
            return EncryptionType.OPEN;
        }
        try {
            return EncryptionType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new PlatformFaultException("Platform returned unknown encryptionType='" + value + "'");
        }
    }
}
