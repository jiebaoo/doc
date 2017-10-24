package com.demo.book;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

@EnableHystrixDashboard
@EnableEurekaClient
@EnableFeignClients
@SpringBootApplication
public class EurekaClientBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaClientBookApplication.class, args);
    }
}
