package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.charging.domain.ZellaCommand;
import com.ndzela.admin.charging.domain.ZellaCommandRepository;
import com.ndzela.admin.charging.domain.ZellaCommandStatus;
import com.ndzela.admin.charging.domain.ZellaCommandType;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.driver.domain.SuspensionCause;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;

// Compares what each Zella customer has been told with what the driver is, and queues the order that makes them agree. Only suspensions for arrears reach Zella.
// A customer told "suspended" is restored once the driver is active again or is no longer linked to that customer; a blocked or deactivated driver stays suspended there.
@Slf4j
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
            attempt(() -> drivers.findById(tag.getDriverId()).ifPresent(driver -> suspendIfNeeded(driver, tag.getCustomerId())), tag.getCustomerId());
        }
        for (String customerId : commands.customersWithOrders()) {
            attempt(() -> restoreIfNeeded(customerId), customerId);
        }
    }

    private void suspendIfNeeded(Driver driver, String customerId) {
        boolean shouldBeSuspended = driver.getStatus() == DriverStatus.SUSPENDED && driver.getSuspensionCause() == SuspensionCause.ARREARS;
        if (shouldBeSuspended && !toldSuspended(customerId)) {
            queue.suspend(driver.getId(), driver.getOverdueAmount().setScale(0, RoundingMode.HALF_UP).longValue());
        }
    }

    private void restoreIfNeeded(String customerId) {
        ZellaCommand last = commands.findFirstByCustomerIdAndStatusNotOrderByCreatedAtDesc(customerId, ZellaCommandStatus.CANCELLED).orElse(null);
        if (last == null || last.getType() != ZellaCommandType.SUSPEND) {
            return;
        }
        boolean stillTheirs = idTags.findByCustomerId(customerId).filter(t -> t.getStatus() == IdTagStatus.LINKED && t.getDriverId().equals(last.getDriverId())).isPresent();
        boolean driverActive = stillTheirs && drivers.findById(last.getDriverId()).map(d -> d.getStatus() == DriverStatus.ACTIVE).orElse(false);
        if (!stillTheirs || driverActive) {
            queue.restore(last.getDriverId(), customerId);
        }
    }

    private boolean toldSuspended(String customerId) {
        return commands.findFirstByCustomerIdAndStatusNotOrderByCreatedAtDesc(customerId, ZellaCommandStatus.CANCELLED)
                .map(command -> command.getType() == ZellaCommandType.SUSPEND).orElse(false);
    }

    private void attempt(Runnable step, String customerId) {
        try {
            step.run();
        } catch (RuntimeException e) {
            log.error("Could not reconcile the Zella customer {}", customerId, e);
        }
    }
}
