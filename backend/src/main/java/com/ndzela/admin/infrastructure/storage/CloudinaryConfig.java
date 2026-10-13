package com.ndzela.admin.infrastructure.storage;

import com.ndzela.admin.config.properties.AppProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

@Configuration
public class CloudinaryConfig {
    @Bean
    public CloudinaryClient cloudinaryClient(AppProperties props, ObjectMapper mapper) {
        var timeout = props.storage().requestTimeout();
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout).build());
        factory.setReadTimeout(timeout);
        return new CloudinaryClient(RestClient.builder().requestFactory(factory).build(), props.storage(), mapper);
    }
}
