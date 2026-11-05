package com.ndzela.admin.infrastructure.zella;

import com.ndzela.admin.config.properties.AppProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

@Configuration
public class ZellaConfig {
    @Bean
    public ZellaClient zellaClient(AppProperties props, ObjectMapper mapper) {
        var zella = props.zella();
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(zella.requestTimeout()).build());
        factory.setReadTimeout(zella.requestTimeout());
        String base = zella.baseUrl() == null || zella.baseUrl().isBlank() ? "http://localhost" : zella.baseUrl();
        return new ZellaClient(RestClient.builder().baseUrl(base).requestFactory(factory).build(), zella, mapper);
    }
}
