package com.ndzela.admin.infrastructure.zella;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// What a linked customer paid for one session, from the Zella wallet; amounts are integer minor units. Any field may be null.
public record ZellaFeedItem(String transactionId, String customerId, String idTag, String sessionState, String paymentState, String paymentMethod,
                            Long energyCostMinor, Long vatMinor, Long serviceFeeMinor, Long totalChargedMinor, String currency, Integer currencyDecimals,
                            boolean breakdownEstimated, OffsetDateTime updatedAt,
                            String stationId, Integer connectorId, OffsetDateTime startedAt, OffsetDateTime stoppedAt, BigDecimal energyKwh,
                            BigDecimal batteryStartPercent, BigDecimal batteryPercent, BigDecimal powerKw, Long pricePerKwhMinor, String raw) {}
