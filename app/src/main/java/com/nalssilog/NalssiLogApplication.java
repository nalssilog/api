package com.nalssilog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NalssiLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(NalssiLogApplication.class, args);
    }
}
