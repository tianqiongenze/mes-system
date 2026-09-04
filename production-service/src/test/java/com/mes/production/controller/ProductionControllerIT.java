package com.mes.production.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.production.dto.ProductionOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductionControllerIT {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void createOrderThenRetrieveIt() throws Exception {
        ProductionOrderRequest req = new ProductionOrderRequest();
        req.setProductCode("P-INT-1");
        req.setQuantity(250);
        req.setPriority("HIGH");
        String body = json.writeValueAsString(req);

        String location = mvc.perform(post("/api/v1/production/orders")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.quantity").value(250))
            .andReturn().getResponse().getContentAsString();

        String id = json.readTree(location).get("id").asText();

        mvc.perform(get("/api/v1/production/orders/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productCode").value("P-INT-1"));

        // start it
        mvc.perform(post("/api/v1/production/orders/" + id + "/start")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // report progress to completion
        mvc.perform(post("/api/v1/production/orders/" + id + "/progress").param("completed", "250").param("defects", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        // yield-rate metric
        mvc.perform(get("/api/v1/production/orders/metrics/yield-rate"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unit").value("percent"));
    }

    @Test
    void rejectsInvalidOrder() throws Exception {
        ProductionOrderRequest req = new ProductionOrderRequest();
        req.setProductCode("");
        req.setQuantity(0);
        mvc.perform(post("/api/v1/production/orders").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }
}
