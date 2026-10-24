package com.perfumeshop.crmemulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CrmEmulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmEmulatorApplication.class, args);
    }
}
