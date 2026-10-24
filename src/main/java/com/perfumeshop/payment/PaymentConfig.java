package com.perfumeshop.payment;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(YooKassaProperties.class)
public class PaymentConfig {
}
