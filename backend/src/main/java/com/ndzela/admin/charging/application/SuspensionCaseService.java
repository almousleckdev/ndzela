package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.api.dto.SuspensionCase;
import com.ndzela.admin.charging.api.dto.SuspensionCase.CaseDriver;
import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.charging.domain.SuspensionEvidence;
import com.ndzela.admin.charging.domain.SuspensionEvidenceRepository;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.driver.application.DriverDirectory;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.application.model.HeldVehicle;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// The cases for staff: who was suspended, who helped them (when that is one of our drivers) and the car each held at the time.
@Service
@RequiredArgsConstructor
public class SuspensionCaseService {
    private static final int MAX_SIZE = 100;
    private static final int WINDOW_HOURS = 24;

    private final SuspensionEvidenceRepository evidence;
    private final DriverDirectory directory;
    private final VehicleService vehicles;
    private final IdTagRepository idTags;
    private final HelperSessionReader helperSessions;

    @Transactional(readOnly = true)
    public PageResponse<SuspensionCase> list(int page, int size) {
        Page<SuspensionEvidence> found = evidence.findAllByOrderByOccurredAtDesc(PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_SIZE))));
        Set<UUID> driverIds = found.stream().flatMap(e -> Stream.of(e.getSuspendedDriverId(), e.getReceivingDriverId())).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, DriverRef> drivers = driverIds.stream().map(directory::find).flatMap(java.util.Optional::stream).collect(Collectors.toMap(DriverRef::id, d -> d));
        List<HeldVehicle> held = directory.held(List.copyOf(driverIds));
        Map<UUID, VehicleResponse> cars = vehicles.responsesByIds(held.stream().map(HeldVehicle::vehicleId).distinct().toList());
        List<SuspensionCase> items = found.stream().map(e -> toCase(e, drivers, held, cars)).toList();
        return new PageResponse<>(items, found.getNumber(), found.getSize(), found.getTotalElements());
    }

    private SuspensionCase toCase(SuspensionEvidence e, Map<UUID, DriverRef> drivers, List<HeldVehicle> held, Map<UUID, VehicleResponse> cars) {
        HelpScore score = scoreOf(e);
        return new SuspensionCase(e.getId(), e.getOccurredAt(), e.getKind(), e.getAmountMinor(), e.getCurrency(), person(e.getSuspendedDriverId(), e, drivers, held, cars),
                person(e.getReceivingDriverId(), e, drivers, held, cars), e.getReceivingRef(), score == null ? null : score.score(),
                score != null && score.flagged() ? score.reasons() : List.of(), score != null && score.flagged());
    }

    // The charges of the helper's account in the 24 hours around the event, against what that account normally takes; null when the helper is not one of our drivers
    private HelpScore scoreOf(SuspensionEvidence e) {
        if (e.getReceivingDriverId() == null) {
            return null;
        }
        String idTag = idTags.findAllByDriverId(e.getReceivingDriverId()).stream().filter(t -> t.getStatus() == IdTagStatus.LINKED).map(IdTag::getIdTag).filter(Objects::nonNull).findFirst().orElse(null);
        if (idTag == null) {
            return null;
        }
        OffsetDateTime from = e.getOccurredAt().minusHours(WINDOW_HOURS);
        BigDecimal normal = helperSessions.normalKwh(idTag, from, 2);
        return HelpScorer.score(helperSessions.between(idTag, from, e.getOccurredAt().plusHours(WINDOW_HOURS)), normal);
    }

    private static CaseDriver person(UUID driverId, SuspensionEvidence e, Map<UUID, DriverRef> drivers, List<HeldVehicle> held, Map<UUID, VehicleResponse> cars) {
        DriverRef driver = driverId == null ? null : drivers.get(driverId);
        if (driver == null) {
            return null;
        }
        VehicleResponse car = held.stream().filter(h -> h.driverId().equals(driverId) && h.heldAt(e.getOccurredAt())).map(h -> cars.get(h.vehicleId())).filter(Objects::nonNull).findFirst().orElse(null);
        return new CaseDriver(driver.id(), driver.fullName(), car == null ? null : car.fleetNumber() + " · " + car.plate());
    }
}
