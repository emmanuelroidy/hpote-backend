package com.hpote.backend;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HpoteBackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void errorDispatchDoesNotBecomeAnAuthenticationFailure() throws Exception {
		mockMvc.perform(get("/error").with(request -> {
			request.setDispatcherType(DispatcherType.ERROR);
			request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 400);
			request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/assistant/translations/resolve");
			return request;
		})).andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
	}

}
