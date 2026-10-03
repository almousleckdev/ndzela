package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.payment.domain.DayIncidentKind;

import java.time.LocalDate;

// An incident as the account computation sees it
public record AccountIncident(LocalDate day, DayIncidentKind kind) {}
