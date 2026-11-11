package com.ndzela.admin.payment.application;

import com.ndzela.admin.charging.application.ZellaCommandQueue;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.payment.application.model.AccountState;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

// Keeps the Zella account in step with the rent: suspended with the driver, restored when everything is paid, suspended again if a payment is reversed.
@Component
@RequiredArgsConstructor
class ArrearsSuspensionSync {
    private final DriverArrearsService driverArrears;
    private final ZellaCommandQueue zella;
    private final ArrearsMailer mailer;

    void suspended(DriverRef driver, AccountState state) {
        zella.suspend(driver.id(), owedMinor(state));
    }

    void settled(DriverRef driver, AccountState state) {
        if (driverArrears.restoreFromArrears(driver.id())) {
            zella.restore(driver.id());
            mailer.send(driver, "votre compte est réactivé", "Votre loyer est entièrement réglé : votre compte est de nouveau actif.", amount(state));
        }
    }

    void unpaidAgain(DriverRef driver, AccountState state) {
        if (driverArrears.suspendAgainAfterRestore(driver.id(), ArrearsEvaluator.SUSPENSION_REASON)) {
            zella.suspend(driver.id(), owedMinor(state));
            mailer.send(driver, "votre compte est suspendu pour impayés", "Un paiement a été annulé : des loyers sont de nouveau impayés et votre compte est suspendu. Régularisez l'intégralité pour le débloquer.", amount(state));
        }
    }

    private static long owedMinor(AccountState state) {
        return state.overdueAmount().setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static String amount(AccountState state) {
        return owedMinor(state) + " FCFA";
    }
}
