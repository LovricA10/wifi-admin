package hr.ht.rnd.wifiadmin.application.exception;

public class PlatformTimeoutException extends RuntimeException {

    public PlatformTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
