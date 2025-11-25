package com.wastemanagement.backend;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.CollectionPointController;
import com.wastemanagement.backend.dto.collectionPoint.CollectionPointRequestDTO;
import com.wastemanagement.backend.dto.collectionPoint.CollectionPointResponseDTO;
import com.wastemanagement.backend.mapper.CollectionPointMapper;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.service.collectionPoint.CollectionPointService;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.security.JwtUtil;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CollectionPointController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
@ActiveProfiles("test")
class CollectionPointControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CollectionPointService collectionPointService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CollectionPointRequestDTO requestDTO;
    private CollectionPointResponseDTO responseDTO;
    private CollectionPoint entity;

    @BeforeEach
    void setup() {
        requestDTO = new CollectionPointRequestDTO();
        requestDTO.setAdresse("Rue 123");
        requestDTO.setActive(true);
        requestDTO.setLocation(new GeoJSONPoint(10.0, 20.0));

        entity = new CollectionPoint(
                "1",
                new GeoJSONPoint(10.0, 20.0),
                true,
                "Rue 123",
                null
        );

        responseDTO = new CollectionPointResponseDTO(
                "1",
                new GeoJSONPoint(10.0, 20.0),
                true,
                "Rue 123",
                null
        );
    }

    // -------------------------------
    // MAPPER TESTS
    // -------------------------------
    @Test
    void testMapperToEntityAndResponse() {
        CollectionPointMapper mapper = new CollectionPointMapper();

        CollectionPoint mapped = mapper.toEntity(requestDTO);

        assert mapped.getAdresse().equals("Rue 123");
        assert mapped.isActive();
        assert mapped.getLocation() != null;
        assert mapped.getLocation().getLongitude() == 10.0;
        assert mapped.getLocation().getLatitude() == 20.0;

        CollectionPointResponseDTO dto = mapper.toResponseDTO(mapped);

        assert dto.getAdresse().equals("Rue 123");
        assert dto.isActive();
        assert dto.getLocation() != null;
        assert dto.getLocation().getLongitude() == 10.0;
        assert dto.getLocation().getLatitude() == 20.0;
    }

    // -------------------------------
    // CONTROLLER TESTS
    // -------------------------------
    @Test
    void testCreateCollectionPoint() throws Exception {
        when(collectionPointService.create(any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/collectionPoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.adresse").value("Rue 123"))
                .andExpect(jsonPath("$.location.coordinates[0]").value(10.0))
                .andExpect(jsonPath("$.location.coordinates[1]").value(20.0));
    }

    @Test
    void testGetAllCollectionPoints() throws Exception {
        CollectionPointResponseDTO cp2 = new CollectionPointResponseDTO(
                "2",
                new GeoJSONPoint(15.0, 25.0),
                false,
                "Rue 456",
                null
        );

        when(collectionPointService.getAll()).thenReturn(Arrays.asList(responseDTO, cp2));

        mockMvc.perform(get("/api/v1/collectionPoints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
    }

    @Test
    void testGetCollectionPointById() throws Exception {
        when(collectionPointService.getById("1")).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/collectionPoints/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adresse").value("Rue 123"));
    }

    @Test
    void testUpdateCollectionPoint() throws Exception {
        CollectionPointResponseDTO updated = new CollectionPointResponseDTO(
                "1",
                new GeoJSONPoint(30.0, 40.0),
                false,
                "Rue 999",
                null
        );

        when(collectionPointService.update(eq("1"), any())).thenReturn(updated);

        requestDTO.setAdresse("Rue 999");
        requestDTO.setActive(false);
        requestDTO.setLocation(new GeoJSONPoint(30.0, 40.0));

        mockMvc.perform(put("/api/v1/collectionPoints/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adresse").value("Rue 999"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.location.coordinates[0]").value(30.0))
                .andExpect(jsonPath("$.location.coordinates[1]").value(40.0));
    }

    @Test
    void testDeleteCollectionPoint() throws Exception {
        doNothing().when(collectionPointService).delete("1");

        mockMvc.perform(delete("/api/v1/collectionPoints/1"))
                .andExpect(status().isNoContent());

        verify(collectionPointService).delete("1");
    }
}
