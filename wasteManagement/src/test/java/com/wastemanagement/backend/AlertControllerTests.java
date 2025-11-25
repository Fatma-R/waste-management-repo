package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.AlertController;
import com.wastemanagement.backend.dto.alert.AlertRequestDTO;
import com.wastemanagement.backend.dto.alert.AlertResponseDTO;
import com.wastemanagement.backend.model.collection.AlertType;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.service.alert.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AlertController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
@ActiveProfiles("test")
class AlertControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertService alertService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AlertRequestDTO requestDTO;
    private AlertResponseDTO responseDTO;

    @BeforeEach
    void setup() {
        requestDTO = new AlertRequestDTO();
        requestDTO.setBinId("bin-1");
        requestDTO.setTs(new Date());
        requestDTO.setType(AlertType.LEVEL);
        requestDTO.setValue(75.0);
        requestDTO.setCleared(false);

        responseDTO = new AlertResponseDTO(
                "1", "bin-1", requestDTO.getTs(),
                AlertType.LEVEL, 75.0, false
        );
    }

    @Test
    void testCreateAlert() throws Exception {
        when(alertService.createAlert(any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.binId").value("bin-1"))
                .andExpect(jsonPath("$.value").value(75.0));
    }

    @Test
    void testGetAllAlerts() throws Exception {
        AlertResponseDTO r2 = new AlertResponseDTO(
                "2", "bin-2", new Date(),
                AlertType.LEVEL, 50.0, true
        );

        when(alertService.getAllAlerts()).thenReturn(Arrays.asList(responseDTO, r2));

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
    }

    @Test
    void testGetAlertById() throws Exception {
        when(alertService.getAlertById("1")).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/alerts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.binId").value("bin-1"));
    }

    @Test
    void testUpdateAlert() throws Exception {
        AlertResponseDTO updated = new AlertResponseDTO(
                "1", "bin-1", new Date(),
                AlertType.LEVEL, 80.0, true
        );

        when(alertService.updateAlert(eq("1"), any())).thenReturn(updated);

        requestDTO.setValue(80.0);
        requestDTO.setCleared(true);

        mockMvc.perform(put("/api/v1/alerts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(80.0))
                .andExpect(jsonPath("$.cleared").value(true));
    }

    @Test
    void testDeleteAlert() throws Exception {
        when(alertService.deleteAlert("1")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/alerts/1"))
                .andExpect(status().isNoContent());

        verify(alertService).deleteAlert("1");
    }

    @Test
    void testGetAlertsByBinId() throws Exception {
        when(alertService.getAlertsByBinId("bin-1")).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/v1/alerts/bin/bin-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].binId").value("bin-1"));
    }

    @Test
    void testGetAlertsByType() throws Exception {
        when(alertService.getAlertsByType("LEVEL")).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/v1/alerts/type/LEVEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("LEVEL"));
    }

    @Test
    void testGetAlertsByCleared() throws Exception {
        when(alertService.getAlertsByCleared(false)).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/v1/alerts/cleared/false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cleared").value(false));
    }
}
