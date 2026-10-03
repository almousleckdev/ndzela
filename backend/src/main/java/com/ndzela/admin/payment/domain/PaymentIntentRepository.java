package com.ndzela.admin.payment.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PaymentIntent i where i.id = :id")
    Optional<PaymentIntent> findForUpdate(@Param("id") UUID id);

    Optional<PaymentIntent> findByIdAndDriverId(UUID id, UUID driverId);

    Optional<PaymentIntent> findFirstByDriverIdAndStatus(UUID driverId, PaymentIntentStatus status);

    List<PaymentIntent> findTop50ByDriverIdOrderByCreatedAtDesc(UUID driverId);

    // PVit's callback and the recheck job know no country yet, so these two look across countries on purpose.
    @Query(value = "select id as id, country_id as countryId from payment_intents where reference = :reference", nativeQuery = true)
    Optional<IntentRef> findRefByReference(@Param("reference") String reference);

    // Waiting for a while, or closed as expired in the last hours (a late confirmation must still be recorded).
    @Query(value = "select id as id, country_id as countryId from payment_intents where (status = 'PENDING' and created_at < :pendingBefore) "
            + "or (status = 'EXPIRED' and completed_at > :expiredSince) order by created_at", nativeQuery = true)
    List<IntentRef> findOpenRefs(@Param("pendingBefore") OffsetDateTime pendingBefore, @Param("expiredSince") OffsetDateTime expiredSince);
}
