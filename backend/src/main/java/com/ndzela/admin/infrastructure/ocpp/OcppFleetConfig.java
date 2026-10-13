package com.ndzela.admin.infrastructure.ocpp;

import com.ndzela.admin.config.properties.AppProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

@Configuration
public class OcppFleetConfig {
    @Bean
    public OcppFleetClient ocppFleetClient(AppProperties props, ObjectMapper mapper) {
        var ocpp = props.ocpp();
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(ocpp.requestTimeout()).build());
        factory.setReadTimeout(ocpp.requestTimeout());
        return new OcppFleetClient(RestClient.builder().baseUrl(ocpp.baseUrl()).requestFactory(factory).build(), ocpp, mapper);
    }
}
