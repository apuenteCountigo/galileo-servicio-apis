package com.galileo.cu.servicioapis;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
@EnableScheduling
@EntityScan({ "com.galileo.cu.commons.models", "com.galileo.cu.commons.dto" })
public class ServicioApisApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ServicioApisApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("**************************************");
        System.out.println("APIS V-2408280502 RECOMPILADO 24-09-28 05:30");
    }

}
