package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.infrastructure.zella.ZellaClient;
import com.ndzela.admin.infrastructure.zella.ZellaException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

// Keeps the Zella app told about our links: a link is sent after an automatic match or a staff confirmation, an unlink when staff refuse it.
// A call that fails is retried by the next run, so the flag on the row is the truth.
@Slf4j
@Component
@RequiredArgsConstructor
public class IdTagZellaSync {
    private final ZellaClient zella;
    private final IdTagRepository tags;
    private final TransactionTemplate transaction;

    /** Sends what is pending; returns how many calls succeeded. */
    public int reconcile() {
        if (!zella.configured()) {
            return 0;
        }
        int done = 0;
        for (IdTag tag : transaction.execute(status -> tags.pendingZellaSync())) {
            boolean link = tag.getStatus() == IdTagStatus.LINKED;
            try {
                if (link) {
                    zella.link(tag.getCustomerId());
                } else {
                    zella.unlink(tag.getCustomerId());
                }
                transaction.executeWithoutResult(status -> tags.findById(tag.getId()).ifPresent(t -> {
                    t.setZellaNotified(link);
                    tags.save(t);
                }));
                done++;
            } catch (ZellaException e) {
                log.warn("Zella was not told about customer {}: {}", tag.getCustomerId(), e.getMessage());
                break;
            }
        }
        return done;
    }
}
