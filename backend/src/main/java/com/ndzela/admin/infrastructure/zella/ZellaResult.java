package com.ndzela.admin.infrastructure.zella;

import java.util.List;

// What Zella found for one driver (`ref`); no match is an empty list
public record ZellaResult(String ref, List<ZellaMatch> matches) {}
