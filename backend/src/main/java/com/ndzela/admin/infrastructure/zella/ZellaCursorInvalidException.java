package com.ndzela.admin.infrastructure.zella;

// PARTNER_CURSOR_INVALID: the saved cursor is refused, so the feed is read again from the start (applying a session twice is harmless)
public class ZellaCursorInvalidException extends ZellaException {
    public ZellaCursorInvalidException() {
        super("The Zella app refused the saved cursor.", null);
    }
}
