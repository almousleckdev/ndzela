package com.ndzela.admin.charging.application;

import java.util.List;

public record HelpScore(int score, List<String> reasons, boolean flagged) {}
