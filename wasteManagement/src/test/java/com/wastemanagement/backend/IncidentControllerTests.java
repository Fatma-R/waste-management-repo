package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.collection.incident.IncidentController;
import com.wastemanagement.backend.dto.collection.incident.GeoJSONPointDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentRequestDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentResponseDTO;
import com.wastemanagement.backend.model.collection.incident.IncidentSeverity;
import com.wastemanagement.backend.model.collection.incident.IncidentStatus;
import com.wastemanagement.backend.model.collection.incident.IncidentType;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.collection.incident.IncidentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.TestConfiguration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = IncidentController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class } )
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
public class IncidentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // -------- MOCKED BEANS --------
    @MockBean
    private IncidentService incidentService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;


    // ================================
    //      CREATE INCIDENT TEST
    // ================================
    @Test
    @DisplayName("Should create incident and return HATEOAS link")
    void testCreateIncident() throws Exception {

        IncidentRequestDTO request = new IncidentRequestDTO();
        request.setType(IncidentType.BLOCKED_STREET);
        request.setSeverity(IncidentSeverity.HIGH);
        request.setStatus(IncidentStatus.OPEN);
        request.setDescription("Blocked street 12");
        GeoJSONPointDTO location = new GeoJSONPointDTO();
        location.setCoordinates(new double[] { 10.1, 36.8 });
        request.setLocation(location);

        IncidentResponseDTO response = new IncidentResponseDTO();
        response.setId("abc123");
        response.setType(IncidentType.BLOCKED_STREET);
        response.setSeverity(IncidentSeverity.HIGH);
        response.setStatus(IncidentStatus.OPEN);
        response.setDescription("Bin broken near street 12");
        response.setReportedAt(Instant.now());

        when(incidentService.createIncident(any())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/incidents")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc123"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }


    // ================================
    //      GET INCIDENT TEST
    // ================================
    @Test
    @DisplayName("Should get incident by id")
    void testGetIncident() throws Exception {

        IncidentResponseDTO response = new IncidentResponseDTO();
        response.setId("xyz999");
        response.setType(IncidentType.TRAFFIC_ACCIDENT);
        response.setSeverity(IncidentSeverity.MEDIUM);
        response.setStatus(IncidentStatus.IN_PROGRESS);
        response.setDescription("Truck broken at highway");

        when(incidentService.getIncident("xyz999")).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/incidents/xyz999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("xyz999"))
                .andExpect(jsonPath("$.type").value("TRAFFIC_ACCIDENT"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }


    // ================================
    //      GET ALL INCIDENTS
    // ================================
    @Test
    @DisplayName("Should return list of incidents")
    void testGetAllIncidents() throws Exception {

        IncidentResponseDTO resp1 = new IncidentResponseDTO();
        resp1.setId("1");

        IncidentResponseDTO resp2 = new IncidentResponseDTO();
        resp2.setId("2");

        when(incidentService.getAllIncidents()).thenReturn(List.of(resp1, resp2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
    }


    // ================================
    //      UPDATE INCIDENT
    // ================================
    @Test
    @DisplayName("Should update incident and return HATEOAS link")
    void testUpdateIncident() throws Exception {

        IncidentRequestDTO updateDto = new IncidentRequestDTO();
        updateDto.setDescription("Updated description");
        updateDto.setStatus(IncidentStatus.RESOLVED);

        IncidentResponseDTO response = new IncidentResponseDTO();
        response.setId("abc777");
        response.setDescription("Updated description");
        response.setStatus(IncidentStatus.RESOLVED);

        when(incidentService.updateIncident(any(), any())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/incidents/abc777")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc777"))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }


    // ================================
    //      DELETE INCIDENT
    // ================================
    @Test
    @DisplayName("Should delete incident")
    void testDeleteIncident() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/incidents/zzz333"))
                .andExpect(status().isOk());
    }

}
