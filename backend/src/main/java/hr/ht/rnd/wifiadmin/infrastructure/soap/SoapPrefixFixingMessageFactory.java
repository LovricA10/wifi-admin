package hr.ht.rnd.wifiadmin.infrastructure.soap;

import jakarta.xml.soap.SOAPException;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.TransportInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

class SoapPrefixFixingMessageFactory extends SaajSoapMessageFactory {

    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP_PREFIX = "soap";

    @Override
    public SaajSoapMessage createWebServiceMessage() {
        SaajSoapMessage message = super.createWebServiceMessage();
        try {
            fixOutboundPrefixes(message.getSaajMessage());
        } catch (SOAPException ignored) {
        }
        return message;
    }

    @Override
    public SaajSoapMessage createWebServiceMessage(InputStream inputStream) throws IOException {
        byte[] bodyBytes = inputStream.readAllBytes();
        int xmlStart = 0;
        while (xmlStart < bodyBytes.length && bodyBytes[xmlStart] != '<') {
            xmlStart++;
        }
        byte[] xmlBytes = xmlStart > 0 ? Arrays.copyOfRange(bodyBytes, xmlStart, bodyBytes.length) : bodyBytes;

        if (inputStream instanceof TransportInputStream transport) {
            return super.createWebServiceMessage(new DelegatingTransportInputStream(transport, xmlBytes));
        }
        return super.createWebServiceMessage(new ByteArrayInputStream(xmlBytes));
    }

    private void fixOutboundPrefixes(jakarta.xml.soap.SOAPMessage message) throws SOAPException {
        var envelope = message.getSOAPPart().getEnvelope();
        envelope.addNamespaceDeclaration(SOAP_PREFIX, SOAP_NS);
        envelope.setPrefix(SOAP_PREFIX);
        message.getSOAPBody().setPrefix(SOAP_PREFIX);
    }

    private static final class DelegatingTransportInputStream extends TransportInputStream {

        private final TransportInputStream delegate;
        private final byte[] bodyBytes;

        DelegatingTransportInputStream(TransportInputStream delegate, byte[] bodyBytes) {
            this.delegate = delegate;
            this.bodyBytes = bodyBytes;
        }

        @Override
        protected InputStream createInputStream() {
            return new ByteArrayInputStream(bodyBytes);
        }

        @Override
        public Iterator<String> getHeaderNames() throws IOException {
            return delegate.getHeaderNames();
        }

        @Override
        public Iterator<String> getHeaders(String name) throws IOException {
            return delegate.getHeaders(name);
        }
    }
}
