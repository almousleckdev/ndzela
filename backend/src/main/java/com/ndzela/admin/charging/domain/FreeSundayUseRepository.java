package com.ndzela.admin.charging.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FreeSundayUseRepository extends Repository<SessionPayment, UUID> {
    @Query(value = "select new com.ndzela.admin.charging.domain.FreeSundayUse(s.id, p.customerId, s.startedAt, s.energyWh, s.stopReason) "
            + "from ChargingSession s, SessionPayment p where p.transactionId = s.transactionId and p.freeCategory = 'FREE_SUNDAY' order by s.startedAt desc, s.id",
            countQuery = "select count(s) from ChargingSession s, SessionPayment p where p.transactionId = s.transactionId and p.freeCategory = 'FREE_SUNDAY'")
    Page<FreeSundayUse> newestFirst(Pageable page);

    @Query("select p.customerId as customerId, max(s.startedAt) as lastStartedAt from ChargingSession s, SessionPayment p "
            + "where p.transactionId = s.transactionId and p.freeCategory = 'FREE_SUNDAY' and p.customerId in :customerIds group by p.customerId")
    List<LastFreeUse> lastUseOf(Collection<String> customerIds);

    interface LastFreeUse {
        String getCustomerId();

        OffsetDateTime getLastStartedAt();
    }
}
