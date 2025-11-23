package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.tournee.TourneeController;
import com.wastemanagement.backend.dto.tournee.RouteStepRequestDTO;
import com.wastemanagement.backend.dto.tournee.RouteStepResponseDTO;
import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.mapper.tournee.RouteStepMapper;
import com.wastemanagement.backend.mapper.tournee.TourneeMapper;
import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.model.tournee.RouteStep;
import com.wastemanagement.backend.model.tournee.StepStatus;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.model.tournee.TourneeStatus;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.service.tournee.TourneeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TourneeController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
@ActiveProfiles("test")
class TourneeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TourneeService tourneeService;

    @MockBean
    private JwtUtil jwtUtils;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TourneeRequestDTO tourneeRequestDTO;
    private Tournee tourneeEntity;
    private TourneeResponseDTO tourneeResponseDTO;

    @BeforeEach
    void setup() {
        // RouteStep request
        RouteStepRequestDTO stepRequestDTO = new RouteStepRequestDTO();
        stepRequestDTO.setOrder(1);
        stepRequestDTO.setStatus(StepStatus.PENDING);
        stepRequestDTO.setPredictedFillPct(50);
        stepRequestDTO.setNotes("Step note");
        stepRequestDTO.setCollectionPointId("CP1");

        RouteStep stepEntity = RouteStepMapper.toEntity(stepRequestDTO);

        // Tournee DTO (request)
        tourneeRequestDTO = new TourneeRequestDTO();
        tourneeRequestDTO.setTourneeType(TrashType.PLASTIC);
        tourneeRequestDTO.setStatus(TourneeStatus.PLANNED);
        tourneeRequestDTO.setPlannedKm(10.5);
        tourneeRequestDTO.setPlannedCO2(2.3);
        tourneeRequestDTO.setStartedAt(new Date());
        tourneeRequestDTO.setFinishedAt(new Date());
        tourneeRequestDTO.setSteps(Collections.singletonList(stepRequestDTO));

        // Tournee entité pour tester le mapper
        tourneeEntity = TourneeMapper.toEntity(tourneeRequestDTO);
        tourneeEntity.setId("1");
        tourneeEntity.setSteps(Collections.singletonList(stepEntity));

        // TourneeResponseDTO simulant la réponse du service
        tourneeResponseDTO = TourneeMapper.toResponse(tourneeEntity);
    }

    @Test
    void testCreateTourneeController() throws Exception {
        when(tourneeService.createTournee(any())).thenReturn(tourneeResponseDTO);

        mockMvc.perform(post("/api/v1/tournees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tourneeRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.tourneeType").value("PLASTIC"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.steps[0].order").value(1))
                .andExpect(jsonPath("$.steps[0].collectionPointId").value("CP1"));

        verify(tourneeService).createTournee(any());
    }

    @Test
    void testGetTourneeByIdController() throws Exception {
        when(tourneeService.getTourneeById("1")).thenReturn(tourneeResponseDTO);

        mockMvc.perform(get("/api/v1/tournees/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.tourneeType").value("PLASTIC"))
                .andExpect(jsonPath("$.steps[0].order").value(1));
    }

    @Test
    void testGetAllTourneesController() throws Exception {
        TourneeResponseDTO t2 = new TourneeResponseDTO();
        t2.setId("2");
        t2.setTourneeType(TrashType.PLASTIC);
        t2.setStatus(TourneeStatus.PLANNED);
        t2.setSteps(Collections.emptyList());

        List<TourneeResponseDTO> list = Arrays.asList(tourneeResponseDTO, t2);

        when(tourneeService.getAllTournees()).thenReturn(list);

        mockMvc.perform(get("/api/v1/tournees")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
    }

    @Test
    void testUpdateTourneeController() throws Exception {
        TourneeResponseDTO updated = new TourneeResponseDTO();
        updated.setId("1");
        updated.setTourneeType(TrashType.ORGANIC);
        updated.setStatus(TourneeStatus.IN_PROGRESS);
        updated.setSteps(tourneeResponseDTO.getSteps());

        tourneeRequestDTO.setStatus(TourneeStatus.IN_PROGRESS);

        when(tourneeService.updateTournee(eq("1"), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/tournees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tourneeRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void testDeleteTourneeController() throws Exception {
        doNothing().when(tourneeService).deleteTournee("1");

        mockMvc.perform(delete("/api/v1/tournees/1"))
                .andExpect(status().isOk());

        verify(tourneeService).deleteTournee("1");
    }
}
