package com.ndzela.admin.config.properties;

import java.util.List;

// The public website: the country its forms belong to, how many it accepts an hour from one visitor, and the origins whose pages may post to the public endpoints
public record PublicSiteProperties(String countryCode, int maxSubmissionsPerHour, List<String> allowedOrigins) {}
