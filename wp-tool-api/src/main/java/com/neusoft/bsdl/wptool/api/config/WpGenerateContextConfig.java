package com.neusoft.bsdl.wptool.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;

@Configuration
public class WpGenerateContextConfig {

    @Bean
    public WPGenerateContext wpGenerateContext() {
        return new WPGenerateContext(WPContext.create());
    }
}
