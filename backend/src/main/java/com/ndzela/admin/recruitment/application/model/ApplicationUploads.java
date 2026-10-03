package com.ndzela.admin.recruitment.application.model;

import org.springframework.web.multipart.MultipartFile;

// The files sent with an application
public record ApplicationUploads(MultipartFile photo, MultipartFile licence, MultipartFile id, MultipartFile addressProof) {}
