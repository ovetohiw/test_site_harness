package com.perfumeshop;

import com.perfumeshop.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(JwtProperties.class)
public class PerfumeShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerfumeShopApplication.class, args);
    }
}
