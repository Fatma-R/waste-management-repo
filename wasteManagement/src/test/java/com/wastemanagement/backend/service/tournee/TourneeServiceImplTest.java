package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.model.collection.BinReading;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.model.tournee.Depot;
import com.wastemanagement.backend.model.tournee.TourneeStatus;
import com.wastemanagement.backend.model.vehicle.Vehicle;
import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import com.wastemanagement.backend.repository.CollectionPointRepository;
import com.wastemanagement.backend.repository.VehicleRepository;
import com.wastemanagement.backend.repository.collection.BinReadingRepository;
import com.wastemanagement.backend.repository.tournee.TourneeRepository;
import com.wastemanagement.backend.vroom.VroomClient;
import com.wastemanagement.backend.vroom.dto.VroomJob;
import com.wastemanagement.backend.vroom.dto.VroomRequest;
import com.wastemanagement.backend.vroom.dto.VroomRoute;
import com.wastemanagement.backend.vroom.dto.VroomSolution;
import com.wastemanagement.backend.vroom.dto.VroomStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourneeServiceImplTest {

    @Mock
    private TourneeRepository tourneeRepository;
    @Mock
    private CollectionPointRepository collectionPointRepository;
    @Mock
    private BinReadingRepository binReadingRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DepotService depotService;
    @Mock
    private VroomClient vroomClient;

    @InjectMocks
    private TourneeServiceImpl tourneeService;

    private Depot mainDepot;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        mainDepot = new Depot("DEPOT", "Main", "addr", new GeoJSONPoint(1.0, 2.0));
        vehicle = new Vehicle("veh1", "ABC-123", 5000, null, null, VehicleStatus.AVAILABLE, false);
    }

    @Test
    void planSingleType_marksPlannedVehicleBusy() {
        when(depotService.getMainDepotEntityOrThrow()).thenReturn(mainDepot);
        when(vehicleRepository.findByStatusAndBusyFalse(VehicleStatus.AVAILABLE))
                .thenReturn(List.of(vehicle));

        // Embedded bin in collection point
        Bin bin = new Bin("bin1", "cp1", true, TrashType.PLASTIC, null);
        CollectionPoint cp = new CollectionPoint(
                "cp1",
                new GeoJSONPoint(1.5, 2.5),
                true,
                "addr",
                new ArrayList<>()
        );
        cp.getBins().add(bin);

        // Service now reads bins from CollectionPoint, so we stub findAll()
        when(collectionPointRepository.findAll()).thenReturn(List.of(cp));
        // It may also call findAllById for selected CPs
        when(collectionPointRepository.findAllById(Set.of("cp1"))).thenReturn(List.of(cp));

        when(binReadingRepository.findTopByBinIdOrderByTsDesc("bin1"))
                .thenReturn(new BinReading("br1", "bin1", new Date(), 80.0, 0, 0.0, 0));

        // No planned / in-progress tours that already cover this CP
        when(tourneeRepository.findByTourneeTypeAndStatus(TrashType.PLASTIC, TourneeStatus.PLANNED))
                .thenReturn(Collections.emptyList());
        when(tourneeRepository.findByTourneeTypeAndStatus(TrashType.PLASTIC, TourneeStatus.IN_PROGRESS))
                .thenReturn(Collections.emptyList());

        // VROOM returns one route with one job
        VroomRoute route = new VroomRoute();
        route.setVehicle(1);
        VroomStep step = new VroomStep();
        step.setType("job");
        step.setJob(1);
        step.setLocation(new double[]{1.5, 2.5});
        route.setSteps(List.of(step));

        VroomSolution solution = new VroomSolution();
        solution.setRoutes(List.of(route));
        solution.setCode(0);
        when(vroomClient.optimize(any(VroomRequest.class))).thenReturn(solution);

        // saveAll(tournees) just returns its argument
        when(tourneeRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> inv.getArgument(0, Iterable.class));

        // markVehiclesBusy() will call findAllById and saveAll on vehicleRepository
        when(vehicleRepository.findAllById(anySet())).thenReturn(List.of(vehicle));

        ArgumentCaptor<List<Vehicle>> saveAllCaptor = ArgumentCaptor.forClass(List.class);
        when(vehicleRepository.saveAll(saveAllCaptor.capture())).thenReturn(List.of(vehicle));

        List<TourneeResponseDTO> result = tourneeService.planTourneesWithVroom(TrashType.PLASTIC, 50.0);

        assertEquals(1, result.size());
        assertEquals("veh1", result.get(0).getPlannedVehicleId());

        List<Vehicle> savedVehicles = saveAllCaptor.getValue();
        assertEquals(1, savedVehicles.size());
        assertTrue(savedVehicles.get(0).isBusy(), "Planned vehicle should be marked busy");
    }

    @Test
    void planSingleType_noVehiclesThrows() {
        when(depotService.getMainDepotEntityOrThrow()).thenReturn(mainDepot);
        when(vehicleRepository.findByStatusAndBusyFalse(VehicleStatus.AVAILABLE))
                .thenReturn(Collections.emptyList());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> tourneeService.planTourneesWithVroom(TrashType.PLASTIC, 50.0));

        assertTrue(ex.getMessage().toLowerCase().contains("no available vehicle"));
    }

    @Test
    void planSingleType_noRoutesFromVroomThrows() {
        when(depotService.getMainDepotEntityOrThrow()).thenReturn(mainDepot);
        when(vehicleRepository.findByStatusAndBusyFalse(VehicleStatus.AVAILABLE))
                .thenReturn(List.of(vehicle));

        // Embedded bin + CP again
        Bin bin = new Bin("bin1", "cp1", true, TrashType.PLASTIC, null);
        CollectionPoint cp = new CollectionPoint(
                "cp1",
                new GeoJSONPoint(1.5, 2.5),
                true,
                "addr",
                new ArrayList<>()
        );
        cp.getBins().add(bin);

        when(collectionPointRepository.findAll()).thenReturn(List.of(cp));
        when(collectionPointRepository.findAllById(Set.of("cp1"))).thenReturn(List.of(cp));

        when(binReadingRepository.findTopByBinIdOrderByTsDesc("bin1"))
                .thenReturn(new BinReading("br1", "bin1", new Date(), 80.0, 0, 0.0, 0));

        when(tourneeRepository.findByTourneeTypeAndStatus(TrashType.PLASTIC, TourneeStatus.PLANNED))
                .thenReturn(Collections.emptyList());
        when(tourneeRepository.findByTourneeTypeAndStatus(TrashType.PLASTIC, TourneeStatus.IN_PROGRESS))
                .thenReturn(Collections.emptyList());

        VroomSolution solution = new VroomSolution();
        solution.setRoutes(Collections.emptyList());
        solution.setCode(0);
        when(vroomClient.optimize(any(VroomRequest.class))).thenReturn(solution);

        assertThrows(IllegalStateException.class,
                () -> tourneeService.planTourneesWithVroom(TrashType.PLASTIC, 50.0));
    }
}
