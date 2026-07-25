package com.mychandha.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.mychandha.platform.identity.PlatformIdentityController;
import com.mychandha.platform.tenancy.OrganizationAccessController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

class ApiRouteContractTest {

    @Test
    void everyApplicationControllerUsesVersionOnePrefix() {
        List<Class<?>> controllers = List.of(
                PlatformIdentityController.class,
                OrganizationAccessController.class);

        assertThat(controllers)
                .allSatisfy(controller -> assertThat(controller
                        .getAnnotation(RequestMapping.class)
                        .value())
                        .allMatch(path -> path.startsWith("/api/v1/")));
    }
}
