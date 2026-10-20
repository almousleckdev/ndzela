package com.ndzela.admin.infrastructure.storage;

import org.springframework.core.io.Resource;

// A stored file, ready to stream: its bytes and the content type to answer with.
public record FileContent(Resource resource, String contentType) {}
