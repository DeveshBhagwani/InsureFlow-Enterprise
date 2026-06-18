package com.insureflow.enterprise.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenAccessingSwagger_thenStatusIsOkOrRedirect() throws Exception {
        // Swagger UI should be allowed publicly, resolving to 3xx redirect or 200 (not 401)
        mockMvc.perform(get("/api/v1/swagger-ui.html").contextPath("/api/v1"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void whenAccessingPublicAuthPath_thenStatusIsNotFoundNotUnauthorized() throws Exception {
        // Auth paths are permitted, so they should return 404 Not Found since no controller is registered yet, not 401
        mockMvc.perform(get("/api/v1/auth/non-existent-route").contextPath("/api/v1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenAccessingProtectedRoute_thenStatusIsUnauthorizedAndApiResponseReturned() throws Exception {
        // Protected paths should be blocked with a 401 and custom ApiResponse JSON structure
        mockMvc.perform(get("/api/v1/policies").contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized - Please authenticate to access this resource"));
    }
}
