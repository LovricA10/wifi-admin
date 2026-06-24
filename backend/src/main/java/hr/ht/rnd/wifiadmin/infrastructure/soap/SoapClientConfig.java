package hr.ht.rnd.wifiadmin.infrastructure.soap;

import hr.ht.rnd.wifiadmin.infrastructure.config.SoapPlatformProperties;
import hr.ht.rnd.wifiadmin.infrastructure.soap.model.GetCpeIdRequest;
import hr.ht.rnd.wifiadmin.infrastructure.soap.model.GetCpeIdResponse;
import hr.ht.rnd.wifiadmin.infrastructure.soap.model.SoapWifiConfiguration;
import hr.ht.rnd.wifiadmin.infrastructure.soap.model.UpdateCpeIdRequest;
import hr.ht.rnd.wifiadmin.infrastructure.soap.model.UpdateCpeIdResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.time.Duration;

@Configuration
public class SoapClientConfig {

    @Bean
    Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(
                GetCpeIdRequest.class,
                GetCpeIdResponse.class,
                UpdateCpeIdRequest.class,
                UpdateCpeIdResponse.class,
                SoapWifiConfiguration.class
        );
        return marshaller;
    }

    @Bean
    WebServiceTemplate webServiceTemplate(Jaxb2Marshaller marshaller, SoapPlatformProperties properties) {
        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
        sender.setConnectionTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        sender.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        template.setMessageSender(sender);
        template.setDefaultUri(properties.url());
        return template;
    }
}
