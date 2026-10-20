package com.hpote.backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.service.service.GraphHopperRoutingService;

class GraphHopperRoutingServiceTest {

    @Test
    void reportsMissingBackendApiKeyBeforeCallingTheProvider() {
        GraphHopperRoutingService routingService = new GraphHopperRoutingService();
        ReflectionTestUtils.setField(routingService, "apiKey", "");

        assertThrows(ApiExceptionDto.class,
                () -> routingService.route(-6.8, 39.2, -6.9, 39.3, "car", "fastest"));
    }

    @Test
    void rejectsInvalidCoordinatesBeforeCallingTheProvider() {
        GraphHopperRoutingService routingService = new GraphHopperRoutingService();
        ReflectionTestUtils.setField(routingService, "apiKey", "test-key");

        assertThrows(ApiExceptionDto.class,
                () -> routingService.route(91, 39.2, -6.9, 39.3, "car", "fastest"));
    }
}
