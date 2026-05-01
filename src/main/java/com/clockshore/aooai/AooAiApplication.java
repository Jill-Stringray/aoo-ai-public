package com.clockshore.aooai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AooAiApplication {

    public static void main(String[] args) {

        SpringApplication.run(AooAiApplication.class, args);
    }

}
