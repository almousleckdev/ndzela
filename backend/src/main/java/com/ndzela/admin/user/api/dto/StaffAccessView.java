package com.ndzela.admin.user.api.dto;

import java.util.UUID;

public record StaffAccessView(boolean linked, UUID vehicleId, String vehicle, String plate, boolean freeCharging) {
    public static final StaffAccessView NONE = new StaffAccessView(false, null, null, null, false);
}
