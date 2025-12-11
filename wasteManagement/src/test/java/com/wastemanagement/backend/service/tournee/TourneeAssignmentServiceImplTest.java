package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeAssignmentResponseDTO;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.model.tournee.TourneeAssignment;
import com.wastemanagement.backend.model.tournee.TourneeStatus;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.user.Skill;
import com.wastemanagement.backend.repository.tournee.TourneeAssignmentRepository;
import com.wastemanagement.backend.repository.tournee.TourneeRepository;
import com.wastemanagement.backend.repository.user.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourneeAssignmentServiceImplTest {

    @Mock
    private TourneeAssignmentRepository assignmentRepository;
    @Mock
    private TourneeRepository tourneeRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private TourneeAssignmentServiceImpl service;

    @Test
    void autoAssignUsesPlannedVehicleAndUpdatesStatus() {
        Tournee tournee = new Tournee();
        tournee.setId("t1");
        tournee.setStatus(TourneeStatus.PLANNED);
        tournee.setPlannedVehicleId("veh-planned");

        when(tourneeRepository.findById("t1")).thenReturn(Optional.of(tournee));
        when(employeeRepository.findAll()).thenReturn(List.of(
                new Employee("e1", null, Skill.DRIVER),
                new Employee("e2", null, Skill.AGENT),
                new Employee("e3", null, Skill.AGENT)
        ));

        ArgumentCaptor<List<TourneeAssignment>> captor = ArgumentCaptor.forClass(List.class);
        when(assignmentRepository.saveAll(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        List<TourneeAssignmentResponseDTO> result = service.autoAssignForTournee("t1");

        assertEquals(3, result.size());
        for (TourneeAssignment saved : captor.getValue()) {
            assertEquals("t1", saved.getTourneeId());
            assertEquals("veh-planned", saved.getVehicleId());
            assertNotNull(saved.getEmployeeId());
            assertNotNull(saved.getShiftStart());
            assertNotNull(saved.getShiftEnd());
        }
        verify(tourneeRepository).save(any(Tournee.class));
        assertEquals(TourneeStatus.IN_PROGRESS, tournee.getStatus());
    }

    @Test
    void autoAssignFailsWhenNotPlanned() {
        Tournee tournee = new Tournee();
        tournee.setId("t1");
        tournee.setStatus(TourneeStatus.COMPLETED);
        when(tourneeRepository.findById("t1")).thenReturn(Optional.of(tournee));

        assertThrows(IllegalStateException.class, () -> service.autoAssignForTournee("t1"));
    }

    @Test
    void autoAssignFailsWhenNotEnoughEmployees() {
        Tournee tournee = new Tournee();
        tournee.setId("t1");
        tournee.setStatus(TourneeStatus.PLANNED);
        when(tourneeRepository.findById("t1")).thenReturn(Optional.of(tournee));
        when(employeeRepository.findAll()).thenReturn(List.of(new Employee()));

        assertThrows(IllegalStateException.class, () -> service.autoAssignForTournee("t1"));
    }
}
