package com.mychandha.platform.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class InboxServiceTest {

    @Test
    void payloadHashIsDeterministic() {
        assertThat(InboxService.sha256("event".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("b8e1f80bd70ae0784c7855a451731b745fddb67749d23f637be9082b75e9575b");
    }
}
