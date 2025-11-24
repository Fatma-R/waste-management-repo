package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.collection.BinController;
import com.wastemanagement.backend.dto.collection.BinRequestDTO;
import com.wastemanagement.backend.dto.collection.BinResponseDTO;
import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.service.collection.BinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = BinController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
@ActiveProfiles("test")
class BinControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtils;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private BinService binService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BinRequestDTO requestDTO;
    private BinResponseDTO responseDTO;

    @BeforeEach
    void setup() {
        requestDTO = new BinRequestDTO();
        requestDTO.setCollectionPointId("CP1");
        requestDTO.setActive(true);
        requestDTO.setType(TrashType.PLASTIC);
        requestDTO.setReadingIds(Arrays.asList("r1", "r2"));
        requestDTO.setAlertIds(Arrays.asList("a1"));

        responseDTO = new BinResponseDTO();
        responseDTO.setId("1");
        responseDTO.setCollectionPointId("CP1");
        responseDTO.setActive(true);
        responseDTO.setType(TrashType.PLASTIC);
        responseDTO.setReadingIds(Arrays.asList("r1", "r2"));
        responseDTO.setAlertIds(Arrays.asList("a1"));
    }

    @Test
    void testCreateBinController() throws Exception {
        when(binService.createBin(any())).thenReturn(responseDTO);

        mockMvc.perform(post("/bins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.collectionPointId").value("CP1"))
                .andExpect(jsonPath("$.type").value("PLASTIC"));
    }

    @Test
    void testUpdateBinController() throws Exception {
        BinResponseDTO updated = new BinResponseDTO();
        updated.setId("1");
        updated.setCollectionPointId("CP2");
        updated.setActive(false);
        updated.setType(TrashType.PLASTIC);
        updated.setReadingIds(Arrays.asList("r3"));
        updated.setAlertIds(Arrays.asList("a2"));

        when(binService.updateBin(eq("1"), any())).thenReturn(Optional.of(updated));

        mockMvc.perform(put("/bins/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionPointId").value("CP2"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.type").value("PLASTIC"));
    }

    @Test
    void testGetBinByIdController() throws Exception {
        when(binService.getBinById("1")).thenReturn(Optional.of(responseDTO));

        mockMvc.perform(get("/bins/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.collectionPointId").value("CP1"))
                .andExpect(jsonPath("$.type").value("PLASTIC"));
    }

    @Test
    void testGetAllBinsController() throws Exception {
        BinResponseDTO d2 = new BinResponseDTO();
        d2.setId("2");
        d2.setCollectionPointId("CP2");
        d2.setActive(false);
        d2.setType(TrashType.PLASTIC);
        d2.setReadingIds(Arrays.asList("x"));
        d2.setAlertIds(Arrays.asList("y"));

        when(binService.getAllBins()).thenReturn(List.of(responseDTO, d2));

        mockMvc.perform(get("/bins")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
    }

    @Test
    void testDeleteBinController() throws Exception {
        when(binService.deleteBin("1")).thenReturn(true);

        mockMvc.perform(delete("/bins/1"))
                .andExpect(status().isNoContent());

        verify(binService).deleteBin("1");
    }
}
