package com.mychandha.platform;

import com.mychandha.platform.identity.IdentityProviderProperties;
import com.mychandha.platform.events.OutboxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Profiles;

@SpringBootApplication
@EnableConfigurationProperties({IdentityProviderProperties.class, OutboxProperties.class})
public class MyChandhaApplication {

    @SuppressWarnings("PMD.CloseResource")
    public static void main(String[] args) {
        // Spring owns the long-lived runtime context and its shutdown hook.
        // Migration mode is the exception and closes explicitly after all
        // application runners complete successfully.
        var context = SpringApplication.run(MyChandhaApplication.class, args);
        if (context.getEnvironment().acceptsProfiles(Profiles.of("migration"))) {
            context.getBean(com.mychandha.platform.runtime.MigrationExitRunner.class)
                    .closeAfterSuccessfulStartup(context);
        }
    }
}
