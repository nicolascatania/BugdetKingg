package com.veritech.BudgetKing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

@Configuration
@ImportHttpServices(basePackages = "com.veritech.BudgetKing")
public class HttpClientConfig {
}
