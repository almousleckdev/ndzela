package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.charging.domain.ZellaCommandRepository;
import com.ndzela.admin.charging.domain.ZellaCommandStatus;
import com.ndzela.admin.charging.domain.ZellaCommandType;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.driver.domain.SuspensionCause;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;

// Compares what each linked driver is with what Zella has been told, and queues the order that makes them agree
// (a driver suspended before this existed, or reactivated by staff). Only suspensions for arrears reach Zella.
@Component
@RequiredArgsConstructor
public class ZellaSuspensionReconciler {
    private final IdTagRepository idTags;
    private final DriverRepository drivers;
    private final ZellaCommandRepository commands;
    private final ZellaCommandQueue queue;

    @Transactional
    public void run() {
        for (IdTag tag : idTags.findAllByStatusOrderByUpdatedAtDesc(IdTagStatus.LINKED)) {
            drivers.findById(tag.getDriverId()).ifPresent(this::align);
        }
    }

    private void align(Driver driver) {
        boolean shouldBeSuspended = driver.getStatus() == DriverStatus.SUSPENDED && driver.getSuspensionCause() == SuspensionCause.ARREARS;
        boolean toldSuspended = commands.findFirstByDriverIdAndStatusNotOrderByCreatedAtDesc(driver.getId(), ZellaCommandStatus.CANCELLED)
                .map(command -> command.getType() == ZellaCommandType.SUSPEND).orElse(false);
        if (shouldBeSuspended && !toldSuspended) {
            queue.suspend(driver.getId(), driver.getOverdueAmount().setScale(0, RoundingMode.HALF_UP).longValue());
        } else if (!shouldBeSuspended && toldSuspended) {
            queue.restore(driver.getId());
        }
    }
}
