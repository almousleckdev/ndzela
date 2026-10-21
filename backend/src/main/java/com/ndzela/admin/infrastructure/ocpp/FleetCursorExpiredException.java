package com.ndzela.admin.infrastructure.ocpp;

// HTTP 410: the saved cursor is no longer valid (the key's scope or grants changed), so the sync must start over.
public class FleetCursorExpiredException extends FleetException {
    public FleetCursorExpiredException(Throwable cause) {
        super("The Fleet API no longer accepts the saved sync cursor (410).", cause);
    }
}
