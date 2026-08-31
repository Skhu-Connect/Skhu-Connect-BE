package org.skhuconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SkhuConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkhuConnectApplication.class, args);
    }

}
