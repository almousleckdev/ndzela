package com.ndzela.admin.charging.application;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

// Signs that one account charged two cars. A car can only lose battery by driving, at a bounded pace, and can only be at one connector at a time.
public final class HelpSignals {
    static final int MAX_DRAIN_POINTS_PER_HOUR = 30;
    static final int TOLERANCE_POINTS = 5;
    static final BigDecimal ENERGY_FACTOR = new BigDecimal("1.5");

    private HelpSignals() {}

    /** Two sessions at two different connectors that run at the same time cannot be one car. */
    public static boolean overlap(List<HelpSession> sessions) {
        for (int i = 0; i < sessions.size(); i++) {
            for (int j = i + 1; j < sessions.size(); j++) {
                HelpSession a = sessions.get(i);
                HelpSession b = sessions.get(j);
                if (!a.sameConnector(b) && startsBeforeEnd(a, b) && startsBeforeEnd(b, a)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The battery of a session must follow from the latest session that ended before it: it can fall only at a bounded pace while driving. A rise proves nothing, the driver may charge elsewhere. */
    public static boolean batteryChainBroken(List<HelpSession> sessions) {
        List<HelpSession> byStart = sessions.stream().sorted(Comparator.comparing(HelpSession::startedAt)).toList();
        for (int i = 0; i < byStart.size(); i++) {
            HelpSession next = byStart.get(i);
            HelpSession previous = byStart.subList(0, i).stream().filter(s -> s.endedBefore(next.startedAt())).max(Comparator.comparing(HelpSession::endedAt)).orElse(null);
            if (previous != null && inconsistent(previous, next)) {
                return true;
            }
        }
        return false;
    }

    /** More than one and a half times the energy the account normally takes over the same period. */
    public static boolean energyAboveNormal(List<HelpSession> sessions, BigDecimal normalKwh) {
        if (normalKwh == null || normalKwh.signum() <= 0) {
            return false;
        }
        BigDecimal total = sessions.stream().map(HelpSession::energyKwh).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.compareTo(normalKwh.multiply(ENERGY_FACTOR)) > 0;
    }

    private static boolean startsBeforeEnd(HelpSession a, HelpSession b) {
        return b.endedAt() == null || a.startedAt().isBefore(b.endedAt());
    }

    private static boolean inconsistent(HelpSession previous, HelpSession next) {
        if (previous.socAfter() == null || next.socBefore() == null) {
            return false;
        }
        double gapHours = Duration.between(previous.endedAt(), next.startedAt()).toMinutes() / 60.0;
        double drop = previous.socAfter().subtract(next.socBefore()).doubleValue();
        double allowedDrop = gapHours * MAX_DRAIN_POINTS_PER_HOUR + TOLERANCE_POINTS;
        return drop > allowedDrop;
    }
}
