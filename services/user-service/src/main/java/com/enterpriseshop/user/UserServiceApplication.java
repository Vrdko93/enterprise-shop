package com.enterpriseshop.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import com.enterpriseshop.web.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;

@EnableCaching
@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
