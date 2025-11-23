package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.tournee.RouteStepController;
import com.wastemanagement.backend.dto.tournee.RouteStepRequestDTO;
import com.wastemanagement.backend.dto.tournee.RouteStepResponseDTO;
import com.wastemanagement.backend.model.tournee.StepStatus;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.service.tournee.RouteStepService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = RouteStepController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
@ActiveProfiles("test")
class RouteStepControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RouteStepService routeStepService;

    @MockBean
    private JwtUtil jwtUtils;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RouteStepRequestDTO stepRequestDTO;
    private RouteStepResponseDTO stepResponseDTO;

    @BeforeEach
    void setup() {
        stepRequestDTO = new RouteStepRequestDTO();
        stepRequestDTO.setOrder(1);
        stepRequestDTO.setStatus(StepStatus.PENDING);
        stepRequestDTO.setPredictedFillPct(50.0);
        stepRequestDTO.setNotes("Step note");
        stepRequestDTO.setCollectionPointId("CP1");

        stepResponseDTO = new RouteStepResponseDTO();
        stepResponseDTO.setId("1");
        stepResponseDTO.setOrder(1);
        stepResponseDTO.setStatus(StepStatus.PENDING);
        stepResponseDTO.setPredictedFillPct(50.0);
        stepResponseDTO.setNotes("Step note");
        stepResponseDTO.setCollectionPointId("CP1");
    }

    @Test
    void testCreateRouteStepController() throws Exception {
        when(routeStepService.createRouteStep(any())).thenReturn(stepResponseDTO);

        mockMvc.perform(post("/api/v1/route-steps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stepRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.order").value(1))
                .andExpect(jsonPath("$.collectionPointId").value("CP1"));

        verify(routeStepService).createRouteStep(any());
    }

    @Test
    void testGetRouteStepByIdController() throws Exception {
        when(routeStepService.getRouteStepById("1")).thenReturn(stepResponseDTO);

        mockMvc.perform(get("/api/v1/route-steps/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.order").value(1));
    }

    @Test
    void testGetAllRouteStepsController() throws Exception {
        RouteStepResponseDTO step2 = new RouteStepResponseDTO();
        step2.setId("2");
        step2.setOrder(2);
        step2.setCollectionPointId("CP2");

        when(routeStepService.getAllRouteSteps()).thenReturn(Arrays.asList(stepResponseDTO, step2));

        mockMvc.perform(get("/api/v1/route-steps")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
    }

    @Test
    void testUpdateRouteStepController() throws Exception {
        RouteStepResponseDTO updated = new RouteStepResponseDTO();
        updated.setId("1");
        updated.setOrder(5);
        updated.setStatus(StepStatus.PENDING);
        updated.setPredictedFillPct(50.0);
        updated.setNotes("Step note");
        updated.setCollectionPointId("CP1");

        stepRequestDTO.setOrder(5);

        when(routeStepService.updateRouteStep(eq("1"), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/route-steps/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stepRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order").value(5));
    }

    @Test
    void testDeleteRouteStepController() throws Exception {
        doNothing().when(routeStepService).deleteRouteStep("1");

        mockMvc.perform(delete("/api/v1/route-steps/1"))
                .andExpect(status().isOk());

        verify(routeStepService).deleteRouteStep("1");
    }
}
