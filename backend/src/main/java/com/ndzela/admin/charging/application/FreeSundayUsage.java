package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.FreeSundayUse;
import com.ndzela.admin.charging.domain.FreeSundayUseRepository;
import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.freecharging.application.FreeChargingUsageSource;
import com.ndzela.admin.freecharging.application.FreeChargingUse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// The free Sunday history: the sessions Zella fed as FREE_SUNDAY, with the NDZela driver the customer is linked to. The tenant filter keeps it to one country.
@Component
@RequiredArgsConstructor
public class FreeSundayUsage implements FreeChargingUsageSource {
    private final FreeSundayUseRepository uses;
    private final IdTagRepository tags;
    private final DriverArrearsService drivers;

    @Override
    @Transactional(readOnly = true)
    public Page<FreeChargingUse> page(int page, int size) {
        Page<FreeSundayUse> found = uses.newestFirst(PageRequest.of(page, size));
        Map<String, UUID> driverOf = driverOfCustomers(found.stream().map(FreeSundayUse::customerId).distinct().toList());
        Map<UUID, DriverRef> people = drivers.refs(driverOf.values());
        return found.map(use -> {
            UUID driverId = driverOf.get(use.customerId());
            DriverRef driver = driverId == null ? null : people.get(driverId);
            return new FreeChargingUse(use.sessionId(), driver == null ? null : driver.id(), driver == null ? null : driver.fullName(),
                    use.startedAt() == null ? null : use.startedAt().toInstant(), use.energyWh() == null ? null : BigDecimal.valueOf(use.energyWh(), 3), use.stopReason());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Instant> lastUseOf(Collection<UUID> driverIds) {
        if (driverIds.isEmpty()) {
            return Map.of();
        }
        Map<String, UUID> driverOf = tags.findAllByDriverIdInAndStatus(driverIds, IdTagStatus.LINKED).stream()
                .collect(Collectors.toMap(IdTag::getCustomerId, IdTag::getDriverId, (first, second) -> first));
        Map<UUID, Instant> last = new HashMap<>();
        uses.lastUseOf(driverOf.keySet()).forEach(use -> last.merge(driverOf.get(use.getCustomerId()), use.getLastStartedAt().toInstant(), (a, b) -> a.isAfter(b) ? a : b));
        return last;
    }

    private Map<String, UUID> driverOfCustomers(List<String> customerIds) {
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        return tags.findAllByCustomerIdInAndStatus(customerIds, IdTagStatus.LINKED).stream()
                .collect(Collectors.toMap(IdTag::getCustomerId, IdTag::getDriverId, (first, second) -> first));
    }
}
