package com.insureflow.enterprise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class InsureFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsureFlowApplication.class, args);
    }
}
