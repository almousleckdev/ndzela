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
        return enqueue(driverId, ZellaCommandType.SUSPEND, owedMinor);
    }

    @Transactional
    public boolean restore(UUID driverId) {
        return enqueue(driverId, ZellaCommandType.RESTORE, 0);
    }

    @Transactional(readOnly = true)
    public long stuck(OffsetDateTime createdBefore) {
        return commands.countByStatusAndCreatedAtBefore(ZellaCommandStatus.PENDING, createdBefore);
    }

    private boolean enqueue(UUID driverId, ZellaCommandType type, long owedMinor) {
        Optional<IdTag> tag = idTags.findAllByDriverId(driverId).stream().filter(t -> t.getStatus() == IdTagStatus.LINKED).findFirst();
        if (tag.isEmpty()) {
            return false;
        }
        List<ZellaCommand> pending = commands.findByDriverIdAndStatus(driverId, ZellaCommandStatus.PENDING);
        pending.stream().filter(c -> c.getType() != type).forEach(c -> c.setStatus(ZellaCommandStatus.CANCELLED));
        Optional<ZellaCommand> same = pending.stream().filter(c -> c.getType() == type).findFirst();
        same.ifPresent(c -> c.setOwedMinor(owedMinor));
        commands.saveAll(pending);
        if (same.isEmpty()) {
            commands.save(ZellaCommand.builder().driverId(driverId).customerId(tag.get().getCustomerId()).type(type).owedMinor(owedMinor)
                    .status(ZellaCommandStatus.PENDING).attempts(0).nextAttemptAt(OffsetDateTime.now()).build());
        }
        return true;
    }
}
