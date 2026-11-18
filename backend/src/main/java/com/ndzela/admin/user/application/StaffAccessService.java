package com.ndzela.admin.user.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.infrastructure.zella.ZellaClient;
import com.ndzela.admin.infrastructure.zella.ZellaDriver;
import com.ndzela.admin.infrastructure.zella.ZellaException;
import com.ndzela.admin.infrastructure.zella.ZellaMatch;
import com.ndzela.admin.user.api.dto.StaffAccessRequest;
import com.ndzela.admin.user.api.dto.StaffAccessView;
import com.ndzela.admin.user.domain.StaffZellaAccess;
import com.ndzela.admin.user.domain.StaffZellaAccessRepository;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.vehicle.domain.Vehicle;
import com.ndzela.admin.vehicle.domain.VehicleRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

// A staff member's Zella account and car, and their free charging: Zella owns the free access, we only ask for it.
@Service
@RequiredArgsConstructor
public class StaffAccessService {
    private static final String UNAVAILABLE = "ZELLA_UNAVAILABLE";

    private final UserRepository users;
    private final StaffZellaAccessRepository access;
    private final VehicleRepository vehicles;
    private final IdTagRepository idTags;
    private final ZellaClient zella;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public StaffAccessView get(UUID userId) {
        users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return access.findById(userId).map(this::view).orElse(StaffAccessView.NONE);
    }

    @Transactional
    public StaffAccessView save(UUID userId, StaffAccessRequest request) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Vehicle vehicle = carOf(request);
        String plate = request.plate() == null || request.plate().isBlank() ? null : request.plate().trim().toUpperCase(Locale.ROOT);
        StaffZellaAccess row = access.findById(userId).orElseGet(() -> StaffZellaAccess.builder().userId(userId).zellaCustomerId(accountOf(user).customerId()).build());
        row.setVehicleId(vehicle == null ? null : vehicle.getId());
        row.setPlate(plate);
        row.setFreeCharging(request.freeCharging());
        send(() -> {
            zella.link(row.getZellaCustomerId());
            if (request.freeCharging()) {
                zella.grantStaffAccess(row.getZellaCustomerId(), reason(user, vehicle, plate));
            } else {
                zella.revokeStaffAccess(row.getZellaCustomerId());
            }
        });
        access.save(row);
        audit.record(request.freeCharging() ? "STAFF_FREE_CHARGING_GRANTED" : "STAFF_ZELLA_ACCESS_SAVED", "USER", userId.toString(), user.getEmail(), user.countryIdOrNull(), reason(user, vehicle, plate));
        return view(row);
    }

    @Transactional
    public void remove(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        StaffZellaAccess row = access.findById(userId).orElse(null);
        if (row == null) {
            return;
        }
        send(() -> {
            zella.revokeStaffAccess(row.getZellaCustomerId());
            if (idTags.findByCustomerId(row.getZellaCustomerId()).isEmpty()) {
                zella.unlink(row.getZellaCustomerId());
            }
        });
        access.delete(row);
        audit.record("STAFF_ZELLA_ACCESS_REMOVED", "USER", userId.toString(), user.getEmail(), user.countryIdOrNull(), null);
    }

    private Vehicle carOf(StaffAccessRequest request) {
        boolean plate = request.plate() != null && !request.plate().isBlank();
        if (request.vehicleId() != null && plate) {
            throw new BusinessRuleViolationException("STAFF_CAR_INVALID", "Choose a fleet car or enter a plate, not both.");
        }
        return request.vehicleId() == null ? null : vehicles.findById(request.vehicleId()).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));
    }

    private ZellaMatch accountOf(User user) {
        if (isBlank(user.getPhoneNumber()) || isBlank(user.getEmail())) {
            throw new BusinessRuleViolationException("STAFF_CONTACT_REQUIRED", "The user needs an e-mail and a phone number to be matched with their Zella account.");
        }
        List<ZellaMatch> found = lookup(new ZellaDriver(user.getId().toString(), user.getEmail().trim().toLowerCase(Locale.ROOT), user.getPhoneNumber().trim()));
        if (found.size() != 1) {
            throw new BusinessRuleViolationException("ZELLA_ACCOUNT_NOT_FOUND", "No single Zella account matches this user's e-mail and phone number.");
        }
        return found.get(0);
    }

    private List<ZellaMatch> lookup(ZellaDriver driver) {
        if (!zella.configured()) {
            throw new BusinessRuleViolationException(UNAVAILABLE, "The Zella app is not reachable.");
        }
        try {
            return zella.lookup(List.of(driver)).stream().flatMap(result -> result.matches().stream()).filter(ZellaMatch::matchedBoth).toList();
        } catch (ZellaException e) {
            throw new BusinessRuleViolationException(UNAVAILABLE, "The Zella app is not reachable.");
        }
    }

    private void send(Runnable call) {
        try {
            call.run();
        } catch (ZellaException e) {
            throw new BusinessRuleViolationException(UNAVAILABLE, "The Zella app is not reachable.");
        }
    }

    private StaffAccessView view(StaffZellaAccess row) {
        Vehicle vehicle = row.getVehicleId() == null ? null : vehicles.findById(row.getVehicleId()).orElse(null);
        return new StaffAccessView(true, row.getVehicleId(), vehicle == null ? null : label(vehicle), row.getPlate(), row.isFreeCharging());
    }

    private static String reason(User user, Vehicle vehicle, String plate) {
        String car = vehicle != null ? label(vehicle) : plate;
        String name = ((user.getFirstName() == null ? "" : user.getFirstName() + " ") + (user.getLastName() == null ? "" : user.getLastName())).trim();
        String text = "Personnel NDZela - " + (name.isEmpty() ? user.getEmail() : name) + (car == null ? "" : " - " + car);
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

    private static String label(Vehicle vehicle) {
        return vehicle.getBrand() + " " + vehicle.getModel() + " (" + vehicle.getPlate() + ")";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
