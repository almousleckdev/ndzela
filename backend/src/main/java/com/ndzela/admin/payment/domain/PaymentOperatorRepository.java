package com.ndzela.admin.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentOperatorRepository extends JpaRepository<PaymentOperator, UUID> {
    List<PaymentOperator> findAllByActiveTrueOrderBySortOrderAscLabelAsc();
}
