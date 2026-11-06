package com.ndzela.admin.payment.application.model;

// How a rent day stands: paid on time or late, unpaid, or ended with its deadline still to come
public enum DayHistoryStatus { PAID_ON_TIME, PAID_LATE, UNPAID, AWAITING }
