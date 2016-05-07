package com.llug.api.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.ComponentScan.Filter;


@Configuration
@ImportResource("classpath:api-server.xml")
@ComponentScan(basePackages = { "com.llug.api",
        "com.llug.api.config",
        "com.llug.api.monitoring",
        "com.llug.api.controllers",
        "com.llug.api.repository",
        "com.llug.api.service" }, excludeFilters = { @Filter(value = ApiServerConfig.class, type = FilterType.ASSIGNABLE_TYPE) })
public class ApiServerConfig {
}