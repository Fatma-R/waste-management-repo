package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.dto.collection.BinRequestDTO;
import com.wastemanagement.backend.dto.collection.BinResponseDTO;
import com.wastemanagement.backend.model.collection.TrashType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BinControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullCrudFlowWithAuth() throws Exception {
        // --- CREATE ---
        BinRequestDTO createRequest = new BinRequestDTO();
        createRequest.setCollectionPointId("cp1");
        createRequest.setActive(true);
        createRequest.setType(TrashType.PLASTIC);

        String createResponse = mockMvc.perform(post("/bins")
                        .with(user("admin").roles("ADMIN")) // simulate logged-in admin
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionPointId").value("cp1"))
                .andExpect(jsonPath("$.type").value("PLASTIC"))
                .andReturn().getResponse().getContentAsString();

        String binId = objectMapper.readTree(createResponse).get("id").asText();

        // --- READ ---
        mockMvc.perform(get("/bins/" + binId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(binId))
                .andExpect(jsonPath("$.collectionPointId").value("cp1"));

        // --- UPDATE ---
        BinRequestDTO updateRequest = new BinRequestDTO();
        updateRequest.setCollectionPointId("cp2");
        updateRequest.setActive(false);
        updateRequest.setType(TrashType.GLASS);

        mockMvc.perform(put("/bins/" + binId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionPointId").value("cp2"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.type").value("GLASS"));

        // --- DELETE ---
        mockMvc.perform(delete("/bins/" + binId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        // --- VERIFY DELETION ---
        mockMvc.perform(get("/bins/" + binId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
