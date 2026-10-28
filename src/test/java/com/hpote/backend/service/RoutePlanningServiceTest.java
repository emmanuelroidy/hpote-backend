package com.hpote.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.service.service.RoutePlanningService;

class RoutePlanningServiceTest {

    private MockRestServiceServer server;
    private RoutePlanningService routingService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        routingService = new RoutePlanningService(
                builder.baseUrl("https://routing.test").build());
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void requestsKeylessValhallaRouteAndAdaptsItForTheMobileClient() {
        server.expect(requestTo("https://routing.test/route"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.costing").value("auto"))
                .andExpect(jsonPath("$.locations[0].lat").value(-6.8))
                .andExpect(jsonPath("$.locations[1].lon").value(39.3))
                .andRespond(withSuccess(routeResponse(9.4, 1200, "Drive northeast."), MediaType.APPLICATION_JSON));

        Map<String, Object> response =
                routingService.route(-6.8, 39.2, -6.9, 39.3, "car", "recommended");
        Map<?, ?> route = (Map<?, ?>) response.get("route");
        List<?> paths = (List<?>) route.get("paths");
        Map<?, ?> path = (Map<?, ?>) paths.get(0);

        assertEquals(9400.0, path.get("distance"));
        assertEquals(1_200_000.0, path.get("time"));
        Map<?, ?> points = (Map<?, ?>) path.get("points");
        List<?> coordinates = (List<?>) points.get("coordinates");
        assertEquals(List.of(0.0, 0.0), coordinates.get(0));
        Map<?, ?> instruction = (Map<?, ?>) ((List<?>) path.get("instructions")).get(0);
        assertEquals("Drive northeast.", instruction.get("text"));
        assertEquals(100.0, instruction.get("distance"));
    }

    @Test
    void selectsShortestAlternativeWhenRequested() {
        server.expect(requestTo("https://routing.test/route"))
                .andExpect(jsonPath("$.costing").value("bicycle"))
                .andExpect(jsonPath("$.alternates").value(1))
                .andRespond(withSuccess("""
                        {
                          "trip": {
                            "status": 0,
                            "summary": {"length": 9.4, "time": 1200},
                            "legs": [{"shape": "????", "maneuvers": [{"instruction": "Main", "length": 9.4, "time": 1200}]}]
                          },
                          "alternates": [{
                            "trip": {
                              "status": 0,
                              "summary": {"length": 8.1, "time": 1500},
                              "legs": [{"shape": "????", "maneuvers": [{"instruction": "Short", "length": 8.1, "time": 1500}]}]
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> response =
                routingService.route(-6.8, 39.2, -6.9, 39.3, "bike", "shortest");
        Map<?, ?> route = (Map<?, ?>) response.get("route");
        Map<?, ?> path = (Map<?, ?>) ((List<?>) route.get("paths")).get(0);

        assertEquals(8100.0, path.get("distance"));
    }

    @Test
    void rejectsUnsupportedPreferencesWithoutCallingTheRoutingService() {
        assertThrows(ApiExceptionDto.class,
                () -> routingService.route(-6.8, 39.2, -6.9, 39.3, "car", "cheapest"));
    }

    @Test
    void rejectsInvalidCoordinatesWithoutCallingTheRoutingService() {
        assertThrows(ApiExceptionDto.class,
                () -> routingService.route(91, 39.2, -6.9, 39.3, "car", "fastest"));
    }

    private String routeResponse(double distanceKm, double timeSeconds, String instruction) {
        return """
                {
                  "trip": {
                    "status": 0,
                    "summary": {"length": %s, "time": %s},
                    "legs": [{
                      "shape": "????",
                      "maneuvers": [{"instruction": "%s", "length": 0.1, "time": 10}]
                    }]
                  }
                }
                """.formatted(distanceKm, timeSeconds, instruction);
    }
}
