package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.charging.domain.ZellaCommand;
import com.ndzela.admin.charging.domain.ZellaCommandRepository;
import com.ndzela.admin.charging.domain.ZellaCommandStatus;
import com.ndzela.admin.charging.domain.ZellaCommandType;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Queues the orders for the Zella app. A driver with no linked Zella customer has nobody to tell, so nothing is queued for them.
@Service
@RequiredArgsConstructor
public class ZellaCommandQueue {
    private final ZellaCommandRepository commands;
    private final IdTagRepository idTags;

    @Transactional
    public boolean suspend(UUID driverId, long owedMinor) {
        return linkedCustomer(driverId).map(customerId -> enqueue(driverId, customerId, ZellaCommandType.SUSPEND, owedMinor)).orElse(false);
    }

    @Transactional
    public boolean restore(UUID driverId) {
        return linkedCustomer(driverId).map(customerId -> enqueue(driverId, customerId, ZellaCommandType.RESTORE, 0)).orElse(false);
    }

    /** Restores a Zella customer the driver is no longer linked to (unlinked or relinked while suspended). */
    @Transactional
    public boolean restore(UUID driverId, String customerId) {
        return enqueue(driverId, customerId, ZellaCommandType.RESTORE, 0);
    }

    @Transactional(readOnly = true)
    public long stuck(OffsetDateTime createdBefore) {
        return commands.countByStatusAndCreatedAtBefore(ZellaCommandStatus.PENDING, createdBefore);
    }

    private Optional<String> linkedCustomer(UUID driverId) {
        return idTags.findAllByDriverId(driverId).stream().filter(t -> t.getStatus() == IdTagStatus.LINKED).map(IdTag::getCustomerId).findFirst();
    }

    private boolean enqueue(UUID driverId, String customerId, ZellaCommandType type, long owedMinor) {
        List<ZellaCommand> pending = commands.findByCustomerIdAndStatus(customerId, ZellaCommandStatus.PENDING);
        pending.stream().filter(c -> c.getType() != type).forEach(c -> c.setStatus(ZellaCommandStatus.CANCELLED));
        Optional<ZellaCommand> same = pending.stream().filter(c -> c.getType() == type).findFirst();
        same.ifPresent(c -> c.setOwedMinor(owedMinor));
        commands.saveAll(pending);
        if (same.isEmpty()) {
            commands.save(ZellaCommand.builder().driverId(driverId).customerId(customerId).type(type).owedMinor(owedMinor)
                    .status(ZellaCommandStatus.PENDING).attempts(0).nextAttemptAt(OffsetDateTime.now()).build());
        }
        return true;
    }
}
