package com.example.appbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(InterviewProperties.class)
public class InterviewConfigBeans {
}
