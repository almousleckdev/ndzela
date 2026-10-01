package com.ndzela.admin.driver.application.model;

import org.springframework.core.io.Resource;

public record DriverFileContent(Resource resource, String contentType) {}
