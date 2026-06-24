package hr.ht.rnd.wifiadmin.infrastructure.soap.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "UpdateCpeIdResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpdateCpeIdResponse {

    private SoapWifiConfiguration configuration;
}
