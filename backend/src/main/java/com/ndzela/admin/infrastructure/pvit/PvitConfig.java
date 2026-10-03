package com.ndzela.admin.infrastructure.pvit;

import com.ndzela.admin.config.properties.AppProperties;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class PvitConfig {
    public static final String REST_CLIENT = "pvitRestClient";

    @Bean(REST_CLIENT)
    public RestClient pvitRestClient(AppProperties props) {
        var timeout = props.pvit().requestTimeout();
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout).build());
        factory.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(props.pvit().baseUrl()).requestFactory(factory).build();
    }

    @Bean
    public PvitSecretProvider pvitSecretProvider(@Qualifier(REST_CLIENT) RestClient client, AppProperties props, tools.jackson.databind.ObjectMapper mapper) {
        return new PvitSecretProvider(client, props.pvit(), mapper);
    }

    @Bean
    public PvitClient pvitClient(@Qualifier(REST_CLIENT) RestClient client, PvitSecretProvider secrets, AppProperties props, tools.jackson.databind.ObjectMapper mapper) {
        return new PvitClient(client, secrets, props.pvit(), mapper);
    }
}
