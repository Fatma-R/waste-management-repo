package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.tournee.DepotController;
import com.wastemanagement.backend.dto.GeoJSONPointDTO;
import com.wastemanagement.backend.dto.tournee.DepotRequestDTO;
import com.wastemanagement.backend.dto.tournee.DepotResponseDTO;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.service.tournee.DepotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = DepotController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
public class DepotControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // -------- MOCKED BEANS --------
    @MockBean
    private DepotService depotService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Should create depot")
    void testCreateDepot() throws Exception {

        DepotRequestDTO request = new DepotRequestDTO();
        request.setName("Main depot");
        request.setAddress("123 Main Street");
        GeoJSONPointDTO location = new GeoJSONPointDTO();
        location.setCoordinates(new double[]{10.1, 36.8});
        request.setLocation(location);

        DepotResponseDTO response = new DepotResponseDTO();
        response.setName("Main depot");
        response.setAddress("123 Main Street");
        response.setLocation(location);

        when(depotService.createDepot(any())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/depots")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Main depot"))
                .andExpect(jsonPath("$.address").value("123 Main Street"));
    }

    @Test
    @DisplayName("Should get depot by id")
    void testGetDepotById() throws Exception {

        DepotResponseDTO response = new DepotResponseDTO();
        response.setName("Secondary depot");
        response.setAddress("456 Second Street");

        when(depotService.getDepotById("dep123")).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/depots/dep123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Secondary depot"))
                .andExpect(jsonPath("$.address").value("456 Second Street"));
    }

    @Test
    @DisplayName("Should return list of depots")
    void testGetAllDepots() throws Exception {

        DepotResponseDTO resp1 = new DepotResponseDTO();
        resp1.setName("Depot 1");

        DepotResponseDTO resp2 = new DepotResponseDTO();
        resp2.setName("Depot 2");

        when(depotService.getAllDepots()).thenReturn(List.of(resp1, resp2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/depots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Depot 1"))
                .andExpect(jsonPath("$[1].name").value("Depot 2"));
    }

    @Test
    @DisplayName("Should update depot")
    void testUpdateDepot() throws Exception {

        DepotRequestDTO updateDto = new DepotRequestDTO();
        updateDto.setName("Updated depot");
        updateDto.setAddress("Updated address");

        DepotResponseDTO response = new DepotResponseDTO();
        response.setName("Updated depot");
        response.setAddress("Updated address");

        when(depotService.updateDepot(any(), any())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/depots/dep999")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated depot"))
                .andExpect(jsonPath("$.address").value("Updated address"));
    }

    @Test
    @DisplayName("Should delete depot")
    void testDeleteDepot() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/depots/depDel"))
                .andExpect(status().isNoContent());
    }
}
