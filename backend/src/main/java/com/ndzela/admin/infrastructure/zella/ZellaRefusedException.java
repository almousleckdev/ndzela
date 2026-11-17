package com.ndzela.admin.infrastructure.zella;

// The Zella app answered a clear no, with its own error code (a charge that is not running, a charger that refused to stop)
public class ZellaRefusedException extends ZellaException {
    private final String code;

    public ZellaRefusedException(String code, Throwable cause) {
        super("The Zella app refused: " + code + ".", cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
