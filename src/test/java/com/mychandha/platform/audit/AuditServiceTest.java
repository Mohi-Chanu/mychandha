package com.mychandha.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditServiceTest {

    @Test
    void canonicalJsonOrdersMapKeysAndHashIsStable() {
        AuditService service = new AuditService(null, new ObjectMapper());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("z", 2);
        data.put("a", 1);

        assertThat(service.canonicalJson(data)).isEqualTo("{\"a\":1,\"z\":2}");
        assertThat(AuditService.sha256("mychandha"))
                .isEqualTo("e1f8ca81b18e182f62a139c3a7abd2f65b668ded33ead3dba65b98be699b7161");
    }
}
