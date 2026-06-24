package hr.ht.rnd.wifiadmin.infrastructure.soap;

import org.springframework.ws.FaultAwareWebServiceMessage;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.WebServiceConnection;

import java.io.IOException;

class SoapBodyFaultAwareWebServiceTemplate extends WebServiceTemplate {

    @Override
    protected boolean hasFault(WebServiceConnection connection, WebServiceMessage response) throws IOException {
        if (response instanceof FaultAwareWebServiceMessage faultMessage && faultMessage.hasFault()) {
            return true;
        }
        return super.hasFault(connection, response);
    }
}
