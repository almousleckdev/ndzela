package com.ndzela.admin.charging.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Adds the signals into one score out of 100; a case is flagged for staff from FLAG_FROM. Nothing here notifies or sanctions anyone.
public final class HelpScorer {
    static final int FLAG_FROM = 60;
    private static final int OVERLAP_POINTS = 60;
    private static final int BATTERY_POINTS = 40;
    private static final int ENERGY_POINTS = 30;
    private static final int MAX_SCORE = 100;

    private HelpScorer() {}

    public static HelpScore score(List<HelpSession> sessions, BigDecimal normalKwh) {
        List<String> reasons = new ArrayList<>();
        int score = 0;
        if (HelpSignals.overlap(sessions)) {
            reasons.add("OVERLAP");
            score += OVERLAP_POINTS;
        }
        if (HelpSignals.batteryChainBroken(sessions)) {
            reasons.add("BATTERY_CHAIN");
            score += BATTERY_POINTS;
        }
        if (HelpSignals.energyAboveNormal(sessions, normalKwh)) {
            reasons.add("ENERGY");
            score += ENERGY_POINTS;
        }
        int capped = Math.min(score, MAX_SCORE);
        return new HelpScore(capped, List.copyOf(reasons), capped >= FLAG_FROM);
    }
}
