package com.ndzela.admin.support;

import com.ndzela.admin.config.properties.ZellaProperties;
import com.ndzela.admin.infrastructure.zella.ZellaClient;
import com.ndzela.admin.infrastructure.zella.ZellaDriver;
import com.ndzela.admin.infrastructure.zella.ZellaEvidencePage;
import com.ndzela.admin.infrastructure.zella.ZellaException;
import com.ndzela.admin.infrastructure.zella.ZellaFeedItem;
import com.ndzela.admin.infrastructure.zella.ZellaFeedPage;
import com.ndzela.admin.infrastructure.zella.ZellaMatch;
import com.ndzela.admin.infrastructure.zella.ZellaResult;

import java.util.ArrayList;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

// The Zella app in memory: it knows a few customers, answers by `ref` like the real one, and remembers who is linked.
public class FakeZellaClient extends ZellaClient {
    private record Customer(String email, String phone, String customerId, String idTag, String firstName, String lastName) {}

    private final List<Customer> customers = new CopyOnWriteArrayList<>();
    private final List<List<ZellaDriver>> asked = new CopyOnWriteArrayList<>();
    private final java.util.Set<String> provenEmails = new java.util.concurrent.CopyOnWriteArraySet<>();
    private final List<String> linked = new CopyOnWriteArrayList<>();
    private final List<String> calls = new CopyOnWriteArrayList<>();
    private volatile boolean down;
    private final Map<String, ZellaFeedPage> feedPages = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<String> feedAsked = new CopyOnWriteArrayList<>();
    private final java.util.Set<String> refusedCursors = new java.util.concurrent.CopyOnWriteArraySet<>();
    private volatile RuntimeException feedFailure;
    private final Map<String, ZellaFeedPage> publicPages = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<String> publicAsked = new CopyOnWriteArrayList<>();
    private volatile RuntimeException publicFailure;
    private final Map<String, ZellaEvidencePage> evidencePages = new java.util.concurrent.ConcurrentHashMap<>();

    public FakeZellaClient(ZellaProperties props) {
        super(null, props, null);
    }

    public void emailProvenByProvider(String customerId) {
        provenEmails.add(customerId);
    }

    @Override
    public boolean configured() {
        return true;
    }

    @Override
    public List<ZellaResult> lookup(List<ZellaDriver> drivers) {
        asked.add(drivers);
        if (down) {
            throw new ZellaException("down", null);
        }
        List<ZellaResult> results = new ArrayList<>();
        for (ZellaDriver d : drivers) {
            List<ZellaMatch> matches = new ArrayList<>();
            for (Customer c : customers) {
                boolean email = c.email().equalsIgnoreCase(d.email());
                boolean phone = c.phone().equals(d.phoneE164());
                if (email && phone) {
                    matches.add(new ZellaMatch(c.customerId(), c.idTag(), Set.of("email", "phone"), "FULL", c.firstName(), c.lastName()));
                } else if (email && provenEmails.contains(c.customerId())) {
                    matches.add(new ZellaMatch(c.customerId(), c.idTag(), Set.of("email"), "FULL", c.firstName(), c.lastName()));
                } else if (email || phone) {
                    matches.add(new ZellaMatch(c.customerId(), c.idTag(), Set.of(email ? "email" : "phone"), "PARTIAL", null, null));
                }
            }
            results.add(new ZellaResult(d.ref(), matches));
        }
        return results;
    }

    @Override
    public void link(String customerId) {
        calls.add("PUT " + customerId);
        if (down) {
            throw new ZellaException("down", null);
        }
        if (!linked.contains(customerId)) {
            linked.add(customerId);
        }
    }

    @Override
    public void suspend(String customerId, long owedMinor, String currency, int decimals) {
        if (down) {
            throw new ZellaException("down", null);
        }
        calls.add("SUSPEND " + customerId + " " + owedMinor);
    }

    @Override
    public void restore(String customerId) {
        if (down) {
            throw new ZellaException("down", null);
        }
        calls.add("RESTORE " + customerId);
    }

    @Override
    public void unlink(String customerId) {
        calls.add("DELETE " + customerId);
        if (down) {
            throw new ZellaException("down", null);
        }
        linked.remove(customerId);
    }

    /** A customer; a null idTag means the customer has not charged yet. */
    public void customer(String idTag, String customerId, String email, String phone, String firstName, String lastName) {
        customers.removeIf(c -> c.customerId().equals(customerId));
        customers.add(new Customer(email, phone, customerId, idTag, firstName, lastName));
    }

    @Override
    public ZellaFeedPage sessions(String cursor, int limit, String customerId) {
        feedAsked.add((customerId == null ? "" : customerId) + "|" + (cursor == null ? "" : cursor));
        if (feedFailure != null) {
            throw feedFailure;
        }
        if (cursor != null && refusedCursors.contains(cursor)) {
            throw new com.ndzela.admin.infrastructure.zella.ZellaCursorInvalidException();
        }
        ZellaFeedPage page = feedPages.get((customerId == null ? "" : customerId) + "|" + (cursor == null ? "" : cursor));
        return page != null ? page : new ZellaFeedPage(List.of(), cursor, false);
    }

    @Override
    public ZellaFeedPage publicSessions(String cursor, int limit) {
        publicAsked.add(cursor == null ? "" : cursor);
        if (publicFailure != null) {
            throw publicFailure;
        }
        ZellaFeedPage page = publicPages.get(cursor == null ? "" : cursor);
        return page != null ? page : new ZellaFeedPage(List.of(), cursor, false);
    }

    @Override
    public ZellaEvidencePage evidence(String cursor, int limit) {
        if (down) {
            throw new ZellaException("down", null);
        }
        ZellaEvidencePage page = evidencePages.get(cursor == null ? "" : cursor);
        return page != null ? page : new ZellaEvidencePage(List.of(), cursor, false);
    }

    /** The page the evidence feed serves for a cursor ("" is the first request). */
    public void serveEvidence(String cursor, ZellaEvidencePage page) {
        evidencePages.put(cursor, page);
    }

    /** The page the public feed serves for a cursor ("" is the first request). */
    public void servePublicFeed(String cursor, ZellaFeedPage page) {
        publicPages.put(cursor, page);
    }

    public void failPublicFeedWith(RuntimeException failure) {
        publicFailure = failure;
    }

    public List<String> publicFeedAsked() {
        return publicAsked;
    }

    /** A session of someone who is not a NDZela driver: no customer, no idTag. */
    public static ZellaFeedItem publicCharge(String transactionId, String stationId, String sessionState, String startedAt, String stoppedAt, String energyKwh, long total, String updatedAt) {
        return new ZellaFeedItem(transactionId, null, null, sessionState, "CHARGED", null, total - 500, 0L, 500L, total, "XAF", 0, false, OffsetDateTime.parse(updatedAt),
                stationId, 1, OffsetDateTime.parse(startedAt), stoppedAt == null ? null : OffsetDateTime.parse(stoppedAt), new java.math.BigDecimal(energyKwh), null, null, null, 350L, "{}");
    }

    /** The page served for a cursor ("" is the first request), for the global feed or one customer's. */
    public void serveFeed(String customerId, String cursor, ZellaFeedPage page) {
        feedPages.put((customerId == null ? "" : customerId) + "|" + cursor, page);
    }

    public void refuseCursor(String cursor) {
        refusedCursors.add(cursor);
    }

    public void failFeedWith(RuntimeException failure) {
        feedFailure = failure;
    }

    public List<String> feedAsked() {
        return feedAsked;
    }

    public static ZellaFeedItem paid(String transactionId, String customerId, String state, long total, String updatedAt) {
        return paidWithTag(transactionId, customerId, "Z-ABC", state, total, updatedAt);
    }

    public static ZellaFeedItem paidWithTag(String transactionId, String customerId, String idTag, String state, long total, String updatedAt) {
        return new ZellaFeedItem(transactionId, customerId, idTag, "COMPLETED", state, "WALLET", total - 500, 0L, 500L, total, "XAF", 0, false, OffsetDateTime.parse(updatedAt),
                null, null, null, null, null, null, null, null, null, null);
    }

    public static ZellaFeedItem charged(String transactionId, String customerId, String sessionState, String stationId, String startedAt, String stoppedAt,
                                        String energyKwh, String batteryStart, String batteryEnd, String powerKw, String updatedAt) {
        return new ZellaFeedItem(transactionId, customerId, "Z-ABC", sessionState, "COMPLETED".equals(sessionState) ? "CHARGED" : "PENDING", "WALLET",
                "COMPLETED".equals(sessionState) ? 3500L : null, 0L, 500L, "COMPLETED".equals(sessionState) ? 4000L : null, "XAF", 0, false, OffsetDateTime.parse(updatedAt),
                stationId, 1, startedAt == null ? null : OffsetDateTime.parse(startedAt), stoppedAt == null ? null : OffsetDateTime.parse(stoppedAt),
                energyKwh == null ? null : new java.math.BigDecimal(energyKwh), batteryStart == null ? null : new java.math.BigDecimal(batteryStart),
                batteryEnd == null ? null : new java.math.BigDecimal(batteryEnd), powerKw == null ? null : new java.math.BigDecimal(powerKw), 350L, "{}");
    }

    public void goDown() {
        down = true;
    }

    public void comeBack() {
        down = false;
    }

    public List<List<ZellaDriver>> asked() {
        return asked;
    }

    public List<String> linked() {
        return linked;
    }

    public List<String> calls() {
        return calls;
    }

    public void clear() {
        customers.clear();
        provenEmails.clear();
        asked.clear();
        linked.clear();
        calls.clear();
        down = false;
        feedPages.clear();
        feedAsked.clear();
        refusedCursors.clear();
        feedFailure = null;
        publicPages.clear();
        publicAsked.clear();
        evidencePages.clear();
        publicFailure = null;
    }
}
