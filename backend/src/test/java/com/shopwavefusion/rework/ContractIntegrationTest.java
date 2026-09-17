package com.shopwavefusion.rework;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContractIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void catalogUsesTheStablePageContract() throws Exception {
        mvc.perform(get("/api/v1/products?page=0&size=12")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12)).andExpect(jsonPath("$.totalItems").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void unknownRouteIsDeniedByDefault() throws Exception {
        mvc.perform(get("/api/v1/legacy/orders")).andExpect(status().isUnauthorized());
    }
}
