package hr.ht.rnd.wifiadmin.application;

import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationRequest;
import hr.ht.rnd.wifiadmin.application.exception.WifiValidationException;
import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import org.springframework.stereotype.Component;

@Component
public class WifiConfigurationValidator {

    public void validate(WifiConfigurationRequest request) {
        EncryptionType encryption = request.encryptionType() != null
                ? request.encryptionType()
                : EncryptionType.OPEN;

        if (encryption != EncryptionType.OPEN) {
            if (request.password() == null || request.password().isBlank()) {
                throw new WifiValidationException(
                        "Password is required when encryption type is " + encryption);
            }
        }
    }
}
