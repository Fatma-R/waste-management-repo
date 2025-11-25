package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.VehicleController;
import com.wastemanagement.backend.dto.vehicle.VehicleRequestDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleResponseDTO;
import com.wastemanagement.backend.mapper.VehicleMapper;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.vehicle.FuelType;
import com.wastemanagement.backend.model.vehicle.Vehicle;
import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.service.vehicle.VehicleService;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = VehicleController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
@ActiveProfiles("test")
class VehicleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private VehicleRequestDTO requestDTO;
    private VehicleResponseDTO responseDTO;
    private Vehicle vehicle;

    @BeforeEach
    void setup() {
        requestDTO = new VehicleRequestDTO();
        requestDTO.setPlateNumber("ABC-123");
        requestDTO.setCapacityVolumeL(5000);
        requestDTO.setCoordinates(new double[]{10.0, 20.0});
        requestDTO.setFuelType(FuelType.DIESEL);
        requestDTO.setStatus(VehicleStatus.AVAILABLE);

        vehicle = new Vehicle(
                "1",
                "ABC-123",
                5000,
                new GeoJSONPoint(10.0, 20.0),
                FuelType.DIESEL,
                VehicleStatus.AVAILABLE
        );

        responseDTO = new VehicleResponseDTO(
                "1",
                "ABC-123",
                5000,
                new double[]{10.0, 20.0},
                FuelType.DIESEL,
                VehicleStatus.AVAILABLE
        );
    }

    // -------------------------------
    // MAPPER TESTS
    // -------------------------------
    @Test
    void testMapperToEntityAndResponse() {
        // Création du DTO d'entrée pour le test
        VehicleRequestDTO requestDTO = new VehicleRequestDTO();
        requestDTO.setPlateNumber("ABC-123");
        requestDTO.setCapacityVolumeL(5000);
        requestDTO.setFuelType(FuelType.DIESEL);
        requestDTO.setCoordinates(new double[]{30.0, 10.0}); // longitude = 30.0, latitude = 10.0
        requestDTO.setStatus(VehicleStatus.AVAILABLE);

        // Création du mapper
        VehicleMapper mapper = new VehicleMapper();

        // Conversion DTO -> Entity
        Vehicle mapped = mapper.toEntity(requestDTO);

        // Vérifications sur l'entité mappée
        assert mapped.getPlateNumber().equals("ABC-123");
        assert mapped.getCapacityVolumeL() == 5000;
        assert mapped.getFuelType() == FuelType.DIESEL;
        assert mapped.getCurrentLocation() != null;
        assert mapped.getCurrentLocation().getLatitude() == 10.0;
        assert mapped.getCurrentLocation().getLongitude() == 30.0;

        // Conversion Entity -> ResponseDTO
        VehicleResponseDTO dto = mapper.toResponseDTO(mapped);

        // Vérifications sur le DTO de réponse
        assert dto.getPlateNumber().equals("ABC-123");
        assert dto.getCapacityVolumeL() == 5000;
        assert dto.getFuelType() == FuelType.DIESEL;
        assert dto.getCoordinates() != null;
        assert dto.getCoordinates()[0] == 30.0; // longitude
        assert dto.getCoordinates()[1] == 10.0; // latitude
    }



    // -------------------------------
    // CONTROLLER TESTS
    // -------------------------------

    @Test
    void testCreateVehicle() throws Exception {
        when(vehicleService.createVehicle(any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.plateNumber").value("ABC-123"))
                .andExpect(jsonPath("$.fuelType").value("DIESEL"));
    }

    @Test
    void testGetAllVehicles() throws Exception {
        VehicleResponseDTO v2 = new VehicleResponseDTO(
                "2", "XYZ-555", 3000,
                new double[]{15.0, 25.0},
                FuelType.GASOLINE, VehicleStatus.IN_SERVICE
        );

        when(vehicleService.getAllVehicles(0, 10)).thenReturn(Arrays.asList(responseDTO, v2));

        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
    }

    @Test
    void testGetVehicleById() throws Exception {
        when(vehicleService.getVehicleById("1")).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value("ABC-123"));
    }

    @Test
    void testUpdateVehicle() throws Exception {
        VehicleResponseDTO updated = new VehicleResponseDTO(
                "1", "NEW-999", 7000,
                new double[]{40.0, 50.0},
                FuelType.ELECTRIC,
                VehicleStatus.IN_SERVICE
        );

        when(vehicleService.updateVehicle(eq("1"), any())).thenReturn(updated);

        requestDTO.setPlateNumber("NEW-999");
        requestDTO.setCapacityVolumeL(7000);
        requestDTO.setFuelType(FuelType.ELECTRIC);

        mockMvc.perform(put("/api/v1/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value("NEW-999"))
                .andExpect(jsonPath("$.fuelType").value("ELECTRIC"));
    }

    @Test
    void testDeleteVehicle() throws Exception {
        when(vehicleService.deleteVehicle("1")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/vehicles/1"))
                .andExpect(status().isNoContent());

        verify(vehicleService).deleteVehicle("1");
    }

    @Test
    void testGetVehiclesByStatus() throws Exception {
        when(vehicleService.getVehiclesByStatus("AVAILABLE")).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/v1/vehicles/status/AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void testGetVehiclesByFuelType() throws Exception {
        when(vehicleService.getVehiclesByFuelType(FuelType.DIESEL)).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/v1/vehicles/fuel/DIESEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fuelType").value("DIESEL"));
    }
}
