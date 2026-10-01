package com.ndzela.admin.driver.application.model;

import org.springframework.web.multipart.MultipartFile;

/** The files sent with a driver creation: the photo is required, the documents are optional. */
public record DriverUploads(MultipartFile photo, MultipartFile licence, MultipartFile id, MultipartFile addressProof) {}
