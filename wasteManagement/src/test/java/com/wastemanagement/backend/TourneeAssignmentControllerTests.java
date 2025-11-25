package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.tournee.TourneeAssignmentController;
import com.wastemanagement.backend.dto.tournee.TourneeAssignmentRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeAssignmentResponseDTO;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.service.tournee.TourneeAssignmentService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TourneeAssignmentController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
@ActiveProfiles("test")
class TourneeAssignmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TourneeAssignmentService tourneeAssignmentService;


    @MockBean
    private JwtUtil jwtUtils;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TourneeAssignmentRequestDTO requestDTO;
    private TourneeAssignmentResponseDTO responseDTO;

    @BeforeEach
    void setup() {
        requestDTO = new TourneeAssignmentRequestDTO();
        requestDTO.setTourneeId("tournee1");
        requestDTO.setEmployeeId("emp1");
        requestDTO.setVehicleId("veh1");
        requestDTO.setShiftStart(new Date());
        requestDTO.setShiftEnd(new Date());

        responseDTO = new TourneeAssignmentResponseDTO();
        responseDTO.setId("1");
        responseDTO.setTourneeId("tournee1");
        responseDTO.setEmployeeId("emp1");
        responseDTO.setVehicleId("veh1");
        responseDTO.setShiftStart(requestDTO.getShiftStart());
        responseDTO.setShiftEnd(requestDTO.getShiftEnd());
    }

    @Test
    void testGetAll() throws Exception {
        List<TourneeAssignmentResponseDTO> list = Arrays.asList(responseDTO);
        when(tourneeAssignmentService.getAll()).thenReturn(list);

        mockMvc.perform(get("/tournee-assignments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].tourneeId").value("tournee1"));

        verify(tourneeAssignmentService).getAll();
    }

    @Test
    void testGetByIdFound() throws Exception {
        when(tourneeAssignmentService.getById("1")).thenReturn(Optional.of(responseDTO));

        mockMvc.perform(get("/tournee-assignments/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.employeeId").value("emp1"));

        verify(tourneeAssignmentService).getById("1");
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        when(tourneeAssignmentService.getById("1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/tournee-assignments/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(tourneeAssignmentService).getById("1");
    }

    @Test
    void testCreate() throws Exception {
        when(tourneeAssignmentService.create(any())).thenReturn(responseDTO);

        mockMvc.perform(post("/tournee-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.vehicleId").value("veh1"));

        verify(tourneeAssignmentService).create(any());
    }

    @Test
    void testUpdateFound() throws Exception {
        TourneeAssignmentResponseDTO updatedDTO = new TourneeAssignmentResponseDTO();
        updatedDTO.setId("1");
        updatedDTO.setTourneeId("tournee2");
        updatedDTO.setEmployeeId("emp2");
        updatedDTO.setVehicleId("veh2");
        updatedDTO.setShiftStart(requestDTO.getShiftStart());
        updatedDTO.setShiftEnd(requestDTO.getShiftEnd());

        when(tourneeAssignmentService.update(eq("1"), any())).thenReturn(Optional.of(updatedDTO));

        mockMvc.perform(put("/tournee-assignments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourneeId").value("tournee2"))
                .andExpect(jsonPath("$.employeeId").value("emp2"));

        verify(tourneeAssignmentService).update(eq("1"), any());
    }

    @Test
    void testUpdateNotFound() throws Exception {
        when(tourneeAssignmentService.update(eq("1"), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/tournee-assignments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());

        verify(tourneeAssignmentService).update(eq("1"), any());
    }

    @Test
    void testDeleteFound() throws Exception {
        when(tourneeAssignmentService.delete("1")).thenReturn(true);

        mockMvc.perform(delete("/tournee-assignments/1"))
                .andExpect(status().isNoContent());

        verify(tourneeAssignmentService).delete("1");
    }

    @Test
    void testDeleteNotFound() throws Exception {
        when(tourneeAssignmentService.delete("1")).thenReturn(false);

        mockMvc.perform(delete("/tournee-assignments/1"))
                .andExpect(status().isNotFound());

        verify(tourneeAssignmentService).delete("1");
    }
}
