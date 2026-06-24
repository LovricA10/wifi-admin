package hr.ht.rnd.wifiadmin.application.exception;

public class CpeNotFoundException extends RuntimeException {

    public CpeNotFoundException(String cpeId) {
        super("CPE not found: " + cpeId);
    }
}
