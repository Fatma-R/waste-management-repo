package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.collection.BinReadingController;
import com.wastemanagement.backend.dto.collection.BinReadingRequestDTO;
import com.wastemanagement.backend.dto.collection.BinReadingResponseDTO;
import com.wastemanagement.backend.service.collection.BinReadingService;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
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
        controllers = BinReadingController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)@ActiveProfiles("test")
class BinReadingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BinReadingService binReadingService;

    @MockBean
    private JwtUtil jwtUtils;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BinReadingRequestDTO requestDTO;
    private BinReadingResponseDTO responseDTO;

    @BeforeEach
    void setup() {
        requestDTO = BinReadingRequestDTO.builder()
                .binId("bin1")
                .ts(new Date())
                .fillPct(50)
                .batteryPct(80)
                .temperatureC(25)
                .signalDbm(-70)
                .build();

        responseDTO = BinReadingResponseDTO.builder()
                .id("reading1")
                .binId("bin1")
                .ts(requestDTO.getTs())
                .fillPct(requestDTO.getFillPct())
                .batteryPct(requestDTO.getBatteryPct())
                .temperatureC(requestDTO.getTemperatureC())
                .signalDbm(requestDTO.getSignalDbm())
                .build();
    }

    @Test
    void testCreateBinReading() throws Exception {
        when(binReadingService.create(any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/bin-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("reading1"))
                .andExpect(jsonPath("$.binId").value("bin1"))
                .andExpect(jsonPath("$.fillPct").value(50));

        verify(binReadingService).create(any());
    }

    @Test
    void testGetBinReadingById() throws Exception {
        when(binReadingService.getById("reading1")).thenReturn(responseDTO);

        mockMvc.perform(get("/api/bin-readings/reading1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("reading1"))
                .andExpect(jsonPath("$.binId").value("bin1"));
    }

    @Test
    void testGetAllBinReadings() throws Exception {
        BinReadingResponseDTO r2 = BinReadingResponseDTO.builder()
                .id("reading2")
                .binId("bin2")
                .ts(new Date())
                .fillPct(70)
                .batteryPct(60)
                .temperatureC(30)
                .signalDbm(-50)
                .build();

        List<BinReadingResponseDTO> list = Arrays.asList(responseDTO, r2);

        when(binReadingService.getAll()).thenReturn(list);

        mockMvc.perform(get("/api/bin-readings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("reading1"))
                .andExpect(jsonPath("$[1].id").value("reading2"));
    }

    @Test
    void testDeleteBinReading() throws Exception {
        doNothing().when(binReadingService).delete("reading1");

        mockMvc.perform(delete("/api/bin-readings/reading1"))
                .andExpect(status().isOk());

        verify(binReadingService).delete("reading1");
    }
}
