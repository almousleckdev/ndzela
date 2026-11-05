package com.ndzela.admin.support;

import com.ndzela.admin.config.properties.AppProperties;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FakeZellaConfig {
    @Bean
    @Primary
    public FakeZellaClient fakeZellaClient(AppProperties props) {
        return new FakeZellaClient(props.zella());
    }
}
