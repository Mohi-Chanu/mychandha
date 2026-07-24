package com.mychandha.platform;

import com.mychandha.platform.identity.IdentityProviderProperties;
import com.mychandha.platform.events.OutboxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({IdentityProviderProperties.class, OutboxProperties.class})
public class MyChandhaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyChandhaApplication.class, args);
    }
}
