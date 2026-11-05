package com.ndzela.admin.charging.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SessionPaymentRepository extends JpaRepository<SessionPayment, UUID> {
    List<SessionPayment> findAllByTransactionIdIn(Collection<String> transactionIds);
}
