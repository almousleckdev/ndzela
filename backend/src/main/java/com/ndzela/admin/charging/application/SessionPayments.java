package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.SessionPayment;
import com.ndzela.admin.charging.domain.SessionPaymentRepository;
import com.ndzela.admin.ocpp.api.SessionPaymentView;
import com.ndzela.admin.ocpp.api.SessionRow;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Puts on each session what the driver paid from the Zella wallet, matched by the transaction id.
@Component
@RequiredArgsConstructor
public class SessionPayments {
    private final SessionPaymentRepository payments;

    public List<SessionRow> mark(List<SessionRow> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        Map<String, SessionPayment> byTransaction = new HashMap<>();
        payments.findAllByTransactionIdIn(rows.stream().map(SessionRow::transactionId).toList()).forEach(p -> byTransaction.put(p.getTransactionId(), p));
        return rows.stream().map(row -> byTransaction.containsKey(row.transactionId()) ? row.withPayment(view(byTransaction.get(row.transactionId()))) : row).toList();
    }

    static SessionPaymentView view(SessionPayment p) {
        int digits = p.getCurrencyDecimals() == null ? 0 : p.getCurrencyDecimals();
        BigDecimal total = whole(p.getTotalChargedMinor(), digits);
        BigDecimal parts = sum(p.getEnergyCostMinor(), p.getVatMinor(), p.getServiceFeeMinor());
        boolean capped = total != null && parts != null && parts.compareTo(total) > 0;
        return new SessionPaymentView(p.getPaymentState(), total, whole(p.getEnergyCostMinor(), digits), whole(p.getVatMinor(), digits), whole(p.getServiceFeeMinor(), digits),
                p.getCurrency(), p.getPaymentMethod(), p.isBreakdownEstimated(), capped);
    }

    private static BigDecimal whole(Long minor, int digits) {
        return minor == null ? null : BigDecimal.valueOf(minor).movePointLeft(Math.max(digits, 0));
    }

    // Null when no part is known
    private static BigDecimal sum(Long... minors) {
        long total = 0;
        boolean any = false;
        for (Long m : minors) {
            if (m != null) {
                total += m;
                any = true;
            }
        }
        return any ? BigDecimal.valueOf(total) : null;
    }
}
