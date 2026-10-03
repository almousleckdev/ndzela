package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.payment.api.dto.OperatorResponse;
import com.ndzela.admin.payment.domain.PaymentOperatorRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentOperatorService {
    private final PaymentOperatorRepository operators;

    // The active operators of the caller's country.
    @Transactional(readOnly = true)
    public List<OperatorResponse> listActive() {
        return operators.findAllByActiveTrueOrderBySortOrderAscLabelAsc().stream().map(OperatorResponse::from).toList();
    }

    // The active operator of the caller's country with this code; a code the country does not offer is refused.
    @Transactional(readOnly = true)
    public OperatorResponse requireActive(String code) {
        return listActive().stream().filter(operator -> operator.code().equals(code)).findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("OPERATOR_NOT_AVAILABLE", "This operator is not available in your country."));
    }
}
