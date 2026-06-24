package hr.ht.rnd.wifiadmin.infrastructure.soap.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapWifiConfiguration {

    private String cpeId;
    private String wifiBand;
    private String ssid;
    private String encryptionType;
    private String password;
}
