package com.ndzela.admin.payment;

import com.ndzela.admin.TestProperties;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.config.properties.PvitProperties;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.infrastructure.pvit.PvitClient;
import com.ndzela.admin.infrastructure.pvit.PvitPaymentRequest;
import com.ndzela.admin.infrastructure.pvit.PvitPaymentStarted;
import com.ndzela.admin.infrastructure.pvit.PvitStatus;
import com.ndzela.admin.payment.api.dto.AccountDetail;
import com.ndzela.admin.payment.api.dto.OperatorResponse;
import com.ndzela.admin.payment.application.PaymentIntentService;
import com.ndzela.admin.payment.application.PaymentIntentStore;
import com.ndzela.admin.payment.application.PaymentOperatorService;
import com.ndzela.admin.payment.application.PaymentQueryService;
import com.ndzela.admin.payment.domain.PaymentIntent;
import com.ndzela.admin.payment.domain.PaymentIntentStatus;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The sandbox rule
class PaymentIntentServiceSandboxTest {
    private final UUID countryId = UUID.randomUUID();
    private final UUID driverId = UUID.randomUUID();
    private final UUID contractId = UUID.randomUUID();

    private BigDecimal amountSentToPvit(BigDecimal sandboxAmount) {
        AppProperties base = TestProperties.create();
        PvitProperties pvit = new PvitProperties("https://pvit.test", "ACC", "pw", "S", "P", "T", "F", "K", "CB", "NDZELA", "LOYER", "CUSTOMER", sandboxAmount,
                Duration.ofSeconds(5), Duration.ofMinutes(3), Duration.ofMinutes(30), false);
        AppProperties props = new AppProperties(base.frontendUrl(), base.corsAllowedOrigins(), base.jwt(), base.auth(), base.otp(), base.bruteForce(), base.mfa(), base.bootstrap(), base.storage(), pvit, base.driverApp(), base.publicSite(), base.ocpp());

        PaymentIntentStore store = mock(PaymentIntentStore.class);
        PaymentOperatorService operators = mock(PaymentOperatorService.class);
        PaymentQueryService queries = mock(PaymentQueryService.class);
        CountryRepository countries = mock(CountryRepository.class);
        PvitClient client = mock(PvitClient.class);

        when(operators.requireActive("AIRTEL_MONEY")).thenReturn(new OperatorResponse("AIRTEL_MONEY", "Airtel Money"));
        when(countries.findById(countryId)).thenReturn(Optional.of(Country.builder().id(countryId).phoneCode("+241").currencyCode("XAF").build()));
        when(queries.account(driverId)).thenReturn(new AccountDetail(driverId, "Awa", "ACTIVE", contractId, "NDZ-1", new BigDecimal("23000"), null,
                new BigDecimal("69000"), BigDecimal.ZERO, null, 0, 0, 3, java.util.List.of(), java.util.List.of()));
        UUID intentId = UUID.randomUUID();
        when(store.createPending(eq(driverId), eq(contractId), eq("AIRTEL_MONEY"), eq("074111111"), any(), eq("XAF"))).thenAnswer(call -> PaymentIntent.builder().id(intentId)
                .reference("NDZREF").amount(call.getArgument(4)).status(PaymentIntentStatus.PENDING).build());
        when(store.find(intentId)).thenAnswer(call -> Optional.of(PaymentIntent.builder().id(intentId).reference("NDZREF").amount(BigDecimal.ONE).status(PaymentIntentStatus.PENDING).build()));
        when(client.startPayment(any())).thenReturn(new PvitPaymentStarted("PAY1", PvitStatus.PENDING, "ok"));

        var service = new PaymentIntentService(store, operators, queries, countries, client, props);
        TenantHolder.runAs(TenantContext.country(countryId), () -> service.start(driverId, "AIRTEL_MONEY", "+24174111111"));

        ArgumentCaptor<PvitPaymentRequest> sent = ArgumentCaptor.forClass(PvitPaymentRequest.class);
        verify(client).startPayment(sent.capture());
        return sent.getValue().amount();
    }

    @Test
    void inSandboxModeTheFixedTestAmountIsCollected() {
        assertEquals(0, new BigDecimal("150").compareTo(amountSentToPvit(new BigDecimal("150"))));
    }

    @Test
    void outsideSandboxModeTheArrearsAreCollected() {
        assertEquals(0, new BigDecimal("69000").compareTo(amountSentToPvit(null)));
    }
}
