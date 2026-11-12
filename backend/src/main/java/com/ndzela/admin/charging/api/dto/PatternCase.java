package com.ndzela.admin.charging.api.dto;

import com.ndzela.admin.charging.api.dto.SuspensionCase.CaseDriver;

import java.util.List;

// An account of ours whose recent charges look like two cars while some drivers are suspended for arrears. Nothing links it to one of them; staff check.
public record PatternCase(CaseDriver account, int score, List<String> reasons, List<String> suspendedDrivers) {}
