package com.ndzela.admin.driver.domain;

import java.util.UUID;

// A driver found at sign-in, with the country to continue in.
public interface LoginCandidate {
    UUID getId();

    UUID getCountryId();
}
