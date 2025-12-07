package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.VehicleController;
import com.wastemanagement.backend.dto.vehicle.VehicleLocationUpdateDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleResponseDTO;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.vehicle.FuelType;
import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.service.vehicle.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = VehicleController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
@ActiveProfiles("test")
class VehicleLocationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private VehicleResponseDTO vehicleResponse;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        GeoJSONPoint location = new GeoJSONPoint(10.0, 20.0);
        vehicleResponse = new VehicleResponseDTO(
                "veh-1",
                "AAA-111",
                4000,
                location,
                FuelType.DIESEL,
                VehicleStatus.AVAILABLE,
                false
        );
    }

    @Test
    void getLocation_ok() throws Exception {
        GeoJSONPoint location = new GeoJSONPoint(10.0, 20.0);
        when(vehicleService.getCurrentLocation("veh-1")).thenReturn(location);

        mockMvc.perform(get("/api/v1/vehicles/veh-1/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coordinates[0]").value(10.0))
                .andExpect(jsonPath("$.coordinates[1]").value(20.0));
    }

    @Test
    void getLocation_notFound() throws Exception {
        when(vehicleService.getCurrentLocation("missing")).thenReturn(null);

        mockMvc.perform(get("/api/v1/vehicles/missing/location"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchLocation_ok() throws Exception {
        VehicleLocationUpdateDTO updateDTO = new VehicleLocationUpdateDTO();
        updateDTO.setLatitude(22.0);
        updateDTO.setLongitude(11.0);

        GeoJSONPoint newLoc = new GeoJSONPoint(11.0, 22.0);
        vehicleResponse.setCurrentLocation(newLoc);
        when(vehicleService.updateCurrentLocation(eq("veh-1"), any())).thenReturn(vehicleResponse);

        mockMvc.perform(patch("/api/v1/vehicles/veh-1/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLocation.coordinates[0]").value(11.0))
                .andExpect(jsonPath("$.currentLocation.coordinates[1]").value(22.0));
    }

    @Test
    void patchLocation_missingField_badRequest() throws Exception {
        VehicleLocationUpdateDTO updateDTO = new VehicleLocationUpdateDTO();
        updateDTO.setLatitude(22.0);
        // longitude missing

        mockMvc.perform(patch("/api/v1/vehicles/veh-1/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void streamLocation_returnsEventStream() throws Exception {
        when(vehicleService.streamLocation("veh-1")).thenReturn(new SseEmitter(0L));

        mockMvc.perform(get("/api/v1/vehicles/veh-1/location/stream"))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }
}
