package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.collection.BinController;
import com.wastemanagement.backend.dto.collection.BinRequestDTO;
import com.wastemanagement.backend.mapper.collection.BinMapper;
import com.wastemanagement.backend.model.collection.Bin;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private Bin bin;

    @BeforeEach
    void setup() {
        requestDTO = new BinRequestDTO();
        requestDTO.setCollectionPointId("CP1");
        requestDTO.setActive(true);
        requestDTO.setType(TrashType.PLASTIC);
        requestDTO.setReadingIds(Arrays.asList("r1", "r2"));
        requestDTO.setAlertIds(Arrays.asList("a1"));

        bin = new Bin();
        bin.setId("1");
        bin.setCollectionPointId("CP1");
        bin.setActive(true);
        bin.setType(TrashType.PLASTIC);
        bin.setReadingIds(requestDTO.getReadingIds());
        bin.setAlertIds(requestDTO.getAlertIds());
    }

    @Test
    void testServiceCreateBin() {
        when(binService.createBin(any())).thenReturn(bin);

        Bin result = binService.createBin(BinMapper.toEntity(requestDTO));

        assert result != null;
        assert result.getCollectionPointId().equals("CP1");
        assert result.getType() == TrashType.PLASTIC;

        verify(binService).createBin(any());
    }

    @Test
    void testMapperToEntityAndResponse() {
        Bin mapped = BinMapper.toEntity(requestDTO);
        assert mapped.getCollectionPointId().equals("CP1");
        assert mapped.getType() == TrashType.PLASTIC;

        var responseDTO = BinMapper.toResponseDTO(bin);
        assert responseDTO.getId().equals("1");
        assert responseDTO.getCollectionPointId().equals("CP1");
        assert responseDTO.getType() == TrashType.PLASTIC;
    }

    @Test
    void testCreateBinController() throws Exception {
        when(binService.createBin(any())).thenReturn(bin);

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
        Bin updated = new Bin();
        updated.setId("1");
        updated.setCollectionPointId("CP2");
        updated.setActive(false);
        updated.setType(TrashType.PLASTIC);
        updated.setReadingIds(Arrays.asList("r3"));
        updated.setAlertIds(Arrays.asList("a2"));

        when(binService.getBinById("1")).thenReturn(Optional.of(bin));
        when(binService.createBin(any())).thenReturn(updated);

        requestDTO.setCollectionPointId("CP2");
        requestDTO.setActive(false);
        requestDTO.setType(TrashType.PLASTIC);
        requestDTO.setReadingIds(Arrays.asList("r3"));
        requestDTO.setAlertIds(Arrays.asList("a2"));

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
        when(binService.getBinById("1")).thenReturn(Optional.of(bin));

        mockMvc.perform(get("/bins/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.collectionPointId").value("CP1"))
                .andExpect(jsonPath("$.type").value("PLASTIC"));
    }

    @Test
    void testGetAllBinsController() throws Exception {
        Bin bin2 = new Bin();
        bin2.setId("2");
        bin2.setCollectionPointId("CP2");
        bin2.setActive(false);
        bin2.setType(TrashType.PLASTIC);
        bin2.setReadingIds(Arrays.asList("r3"));
        bin2.setAlertIds(Arrays.asList("a2"));

        List<Bin> bins = Arrays.asList(bin, bin2);
        when(binService.getAllBins()).thenReturn(bins);

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
