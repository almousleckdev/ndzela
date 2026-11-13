package com.ndzela.admin.charging.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.driver.application.DriverDirectory;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.infrastructure.zella.ZellaClient;
import com.ndzela.admin.infrastructure.zella.ZellaDriver;
import com.ndzela.admin.infrastructure.zella.ZellaException;
import com.ndzela.admin.infrastructure.zella.ZellaMatch;
import com.ndzela.admin.infrastructure.zella.ZellaResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// Finds which Zella customer is which NDZela driver. E-mail, phone or both link by themselves when they point to one Zella customer; two different customers matching a driver are suggestions for staff, except one whose e-mail Google or Apple proved (FULL), which links by itself.
@Slf4j
@Service
@RequiredArgsConstructor
public class IdTagLinkService {
    static final int BATCH = 200;
    private static final int PER_RUN = 1000;
    private static final String SYSTEM = "system";

    private final ZellaClient zella;
    private final IdTagRepository tags;
    private final DriverDirectory directory;
    private final AuditService audit;
    private final TransactionTemplate transaction;
    private final IdTagZellaSync sync;

    /** Looks up the drivers of the caller's country who are neither linked (with their idTag) nor refused. Returns how many links or suggestions were stored. */
    public int lookupMissing() {
        if (!zella.configured()) {
            return 0;
        }
        int stored = lookup(directory.candidates(tags.decidedDriverIds(), PER_RUN));
        sync.reconcile();
        return stored;
    }

    /** Looks up one driver, for example right after creation. */
    public int lookupDriver(UUID driverId) {
        if (!zella.configured()) {
            return 0;
        }
        int stored = directory.find(driverId).filter(d -> !tags.decidedDriverIds().contains(d.id())).map(d -> lookup(List.of(d))).orElse(0);
        sync.reconcile();
        return stored;
    }

    private int lookup(List<DriverRef> drivers) {
        int stored = 0;
        for (int from = 0; from < drivers.size(); from += BATCH) {
            List<DriverRef> batch = drivers.subList(from, Math.min(from + BATCH, drivers.size()));
            List<ZellaDriver> asked = batch.stream().filter(IdTagLinkService::askable).map(d -> new ZellaDriver(d.id().toString(), d.email().trim().toLowerCase(Locale.ROOT), d.phone())).toList();
            if (asked.isEmpty()) {
                continue;
            }
            try {
                List<ZellaResult> found = zella.lookup(asked);
                stored += transaction.execute(status -> store(batch, found));
            } catch (ZellaException e) {
                log.warn("Zella lookup failed: {}", e.getMessage());
                return stored;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.info("Zella lookup raced with another one, the next run settles it");
            }
        }
        return stored;
    }

    // One bad contact makes Zella refuse the whole batch, so only well-formed ones are sent
    private static boolean askable(DriverRef d) {
        return d.email() != null && d.phone() != null && d.email().trim().matches("[^\\s@]+@[^\\s@]+") && d.phone().matches("\\+[1-9]\\d{7,14}");
    }

    private int store(List<DriverRef> batch, List<ZellaResult> results) {
        int stored = 0;
        for (ZellaResult result : results) {
            DriverRef driver = batch.stream().filter(d -> d.id().toString().equals(result.ref())).findFirst().orElse(null);
            if (driver == null) {
                continue;
            }
            long proven = result.matches().stream().filter(ZellaMatch::matchedBoth).count();
            for (ZellaMatch match : result.matches()) {
                boolean unambiguous = result.matches().size() == 1 || (proven == 1 && match.matchedBoth());
                if (upsert(driver, match, unambiguous)) {
                    stored++;
                }
            }
        }
        return stored;
    }

    // True when a row was created or improved (linked, or its idTag arrived)
    private boolean upsert(DriverRef driver, ZellaMatch match, boolean unambiguous) {
        String matchedOn = String.join("+", match.matchedOn().stream().sorted().toList());
        IdTag existing = tags.findByCustomerId(match.customerId()).orElse(null);
        if (existing != null) {
            return refresh(existing, driver, match, matchedOn, unambiguous);
        }
        if (match.idTag() != null && tags.findByIdTag(match.idTag()).isPresent()) {
            return false;
        }
        boolean free = unambiguous && !tags.existsByDriverIdAndStatusAndIdNot(driver.id(), IdTagStatus.LINKED, new UUID(0L, 0L));
        IdTag created = IdTag.builder().idTag(match.idTag()).customerId(match.customerId()).driverId(driver.id()).firstName(match.firstName()).lastName(match.lastName())
                .status(free ? IdTagStatus.LINKED : IdTagStatus.SUGGESTED).matchedOn(matchedOn).decidedBy(SYSTEM).decidedAt(OffsetDateTime.now()).build();
        tags.save(created);
        audit.record(free ? "ID_TAG_LINKED" : "ID_TAG_SUGGESTED", "DRIVER", driver.id().toString(), driver.fullName(), driver.countryId(), (free ? "automatic, " : "to confirm, ") + matchedOn + ", customer " + match.customerId());
        return true;
    }

    private boolean refresh(IdTag existing, DriverRef driver, ZellaMatch match, String matchedOn, boolean unambiguous) {
        if (!existing.getDriverId().equals(driver.id())) {
            return false;
        }
        boolean changed = false;
        if (match.idTag() != null && !match.idTag().equals(existing.getIdTag()) && tags.findByIdTag(match.idTag()).isEmpty()) {
            existing.setIdTag(match.idTag());
            changed = true;
        }
        if (match.firstName() != null) {
            existing.setFirstName(match.firstName());
            existing.setLastName(match.lastName());
        }
        if (existing.getStatus() == IdTagStatus.SUGGESTED && unambiguous && !tags.existsByDriverIdAndStatusAndIdNot(driver.id(), IdTagStatus.LINKED, existing.getId())) {
            existing.setMatchedOn(matchedOn);
            decide(existing, IdTagStatus.LINKED, SYSTEM, "ID_TAG_LINKED", driver, "automatic, " + matchedOn + ", customer " + match.customerId());
            changed = true;
        }
        tags.save(existing);
        return changed;
    }

    /** Staff confirm a suggestion (or link again a refused one). */
    @Transactional
    public void confirm(UUID id, String actor) {
        IdTag tag = load(id);
        if (tag.getStatus() == IdTagStatus.LINKED) {
            return;
        }
        if (tags.existsByDriverIdAndStatusAndIdNot(tag.getDriverId(), IdTagStatus.LINKED, tag.getId())) {
            throw new BusinessRuleViolationException("DRIVER_ALREADY_LINKED", "This driver is already linked to another charging account. Unlink it first.");
        }
        DriverRef driver = directory.find(tag.getDriverId()).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
        decide(tag, IdTagStatus.LINKED, actor, "ID_TAG_LINKED", driver, "confirmed by staff, idTag " + tag.getIdTag());
        tags.save(tag);
    }

    /** Only staff unlink, and the idTag is not linked again by itself. */
    @Transactional
    public void unlink(UUID id, String reason, String actor) {
        IdTag tag = load(id);
        if (tag.getStatus() == IdTagStatus.UNLINKED) {
            return;
        }
        DriverRef driver = directory.find(tag.getDriverId()).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
        decide(tag, IdTagStatus.UNLINKED, actor, "ID_TAG_UNLINKED", driver, "reason: " + reason.trim() + ", customer " + tag.getCustomerId());
        tags.save(tag);
    }

    private void decide(IdTag tag, IdTagStatus status, String by, String action, DriverRef driver, String details) {
        tag.setStatus(status);
        tag.setDecidedBy(by);
        tag.setDecidedAt(OffsetDateTime.now());
        audit.record(action, "DRIVER", driver.id().toString(), driver.fullName(), driver.countryId(), details);
    }

    private IdTag load(UUID id) {
        return tags.findById(id).orElseThrow(() -> new ResourceNotFoundException("Charging account not found."));
    }
}
