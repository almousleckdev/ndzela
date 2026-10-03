package com.ndzela.admin.payment.application.model;

// A receipt ready to be sent
public record PaymentReceiptFile(String filename, byte[] pdf) {}
