package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;

// Shared setup and helpers.
abstract class AbstractContractTest extends AbstractDriverTest {
    protected String center;

    @BeforeEach
    void setup() throws Exception {
        center = newCenter(manager, "NDZELA");
    }
}
