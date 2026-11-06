package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.charging.domain.SessionPayment;
import com.ndzela.admin.charging.domain.SessionPaymentRepository;
import com.ndzela.admin.infrastructure.zella.ZellaClient;
import com.ndzela.admin.infrastructure.zella.ZellaCursorInvalidException;
import com.ndzela.admin.infrastructure.zella.ZellaException;
import com.ndzela.admin.infrastructure.zella.ZellaFeedItem;
import com.ndzela.admin.infrastructure.zella.ZellaFeedPage;
import com.ndzela.admin.infrastructure.zella.ZellaRateLimitedException;

import com.ndzela.admin.tenant.TenantHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

// Keeps what drivers paid in step with the Zella app's feed: it resumes from a saved cursor, reads a customer's history once after linking,
// and applies items by version, so replaying a page is harmless. One run at a time across all instances.
@Slf4j
@Service
@RequiredArgsConstructor
public class ZellaFeedService {
    private static final String LOCK = "zella:feed:lock";
    private static final String BACKOFF = "zella:feed:backoff";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final int PAGE = 200;
    private static final int MAX_PAGES = 100;

    private final ZellaClient zella;
    private final SessionPaymentRepository payments;
    private final ZellaSessionWriter sessionWriter;
    private final DeletedSessions deleted;
    private final IdTagRepository tags;
    private final ZellaFeedState state;
    private final TransactionTemplate transaction;
    private final StringRedisTemplate redis;

    /** Reads the feed, then the history of customers just linked. Returns how many payments were created or updated. */
    public int sync(UUID countryId) {
        if (!zella.configured() || Boolean.TRUE.equals(redis.hasKey(BACKOFF))) {
            return 0;
        }
        String token = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(LOCK, token, LOCK_TTL))) {
            return 0;
        }
        try {
            state.attempted(countryId);
            int changed = feed(countryId) + histories() + publicFeed(countryId);
            state.succeeded(countryId);
            return changed;
        } catch (ZellaRateLimitedException e) {
            redis.opsForValue().set(BACKOFF, "1", e.retryAfter());
            state.failed(countryId, e.getMessage());
            log.warn("Zella feed paused: {}", e.getMessage());
        } catch (ZellaException e) {
            state.failed(countryId, e.getMessage());
            log.error("Zella feed failed: {}", e.getMessage(), e);
        } finally {
            if (token.equals(redis.opsForValue().get(LOCK))) {
                redis.delete(LOCK);
            }
        }
        return 0;
    }

    private int feed(UUID countryId) {
        return readAll(state.cursor(countryId), cursor -> zella.sessions(cursor, PAGE, null), this::store, next -> state.saveCursor(countryId, next));
    }

    // The sessions of people who are not NDZela drivers only fill the charging pages: there is no payment of a driver to keep
    private int publicFeed(UUID countryId) {
        return readAll(state.publicCursor(countryId), cursor -> zella.publicSessions(cursor, PAGE), items -> {
            sessionWriter.store(items);
            return items.size();
        }, next -> state.savePublicCursor(countryId, next));
    }

    // A cursor Zella refuses is dropped and the feed starts over
    private int readAll(String saved, Function<String, ZellaFeedPage> fetch, ToIntFunction<List<ZellaFeedItem>> keep, Consumer<String> saveCursor) {
        try {
            return pages(saved, fetch, keep, saveCursor);
        } catch (ZellaCursorInvalidException e) {
            if (saved == null) {
                throw e;
            }
            log.warn("Zella refused the saved cursor, reading the feed again from the start");
            saveCursor.accept(null);
            return pages(null, fetch, keep, saveCursor);
        }
    }

    private int pages(String cursor, Function<String, ZellaFeedPage> fetch, ToIntFunction<List<ZellaFeedItem>> keep, Consumer<String> saveCursor) {
        int changed = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            ZellaFeedPage result = fetch.apply(cursor);
            String next = result.nextCursor();
            changed += transaction.execute(status -> {
                int n = keep.applyAsInt(result.items());
                if (next != null) {
                    saveCursor.accept(next);
                }
                return n;
            });
            cursor = next != null ? next : cursor;
            if (!result.hasMore()) {
                break;
            }
        }
        return changed;
    }

    // The global cursor has already moved past a newly linked customer's earlier sessions, so they are read once with their own cursor
    private int histories() {
        int changed = 0;
        List<IdTag> pending = transaction.execute(status -> tags.findAllByStatusAndZellaNotifiedAndHistoryRead(IdTagStatus.LINKED, true, false));
        for (IdTag tag : pending) {
            String cursor = null;
            for (int page = 0; page < MAX_PAGES; page++) {
                ZellaFeedPage result = zella.sessions(cursor, PAGE, tag.getCustomerId());
                changed += transaction.execute(status -> store(result.items()));
                cursor = result.nextCursor() != null ? result.nextCursor() : cursor;
                if (!result.hasMore()) {
                    break;
                }
            }
            transaction.executeWithoutResult(status -> tags.findById(tag.getId()).ifPresent(t -> {
                t.setHistoryRead(true);
                tags.save(t);
            }));
        }
        return changed;
    }

    private int store(List<ZellaFeedItem> received) {
        List<ZellaFeedItem> items = deleted.withoutDeleted(TenantHolder.requireCountry(), received, ZellaFeedItem::transactionId);
        sessionWriter.store(items);
        Map<String, SessionPayment> known = new HashMap<>();
        payments.findAllByTransactionIdIn(items.stream().map(ZellaFeedItem::transactionId).toList()).forEach(p -> known.put(p.getTransactionId(), p));
        int changed = 0;
        for (ZellaFeedItem item : items) {
            SessionPayment payment = known.get(item.transactionId());
            if (payment == null) {
                payment = SessionPayment.builder().transactionId(item.transactionId()).customerId(item.customerId()).build();
            } else if (payment.getSourceUpdatedAt() != null && item.updatedAt() != null && payment.getSourceUpdatedAt().isAfter(item.updatedAt())) {
                continue;
            }
            copy(item, payment);
            known.put(item.transactionId(), payments.save(payment));
            changed++;
        }
        return changed;
    }

    private static void copy(ZellaFeedItem item, SessionPayment p) {
        p.setCustomerId(item.customerId());
        p.setIdTag(item.idTag());
        p.setSessionState(item.sessionState());
        p.setPaymentState(item.paymentState());
        p.setPaymentMethod(item.paymentMethod());
        p.setEnergyCostMinor(item.energyCostMinor());
        p.setVatMinor(item.vatMinor());
        p.setServiceFeeMinor(item.serviceFeeMinor());
        p.setTotalChargedMinor(item.totalChargedMinor());
        p.setCurrency(item.currency());
        p.setCurrencyDecimals(item.currencyDecimals());
        p.setBreakdownEstimated(item.breakdownEstimated());
        p.setSourceUpdatedAt(item.updatedAt());
    }
}
