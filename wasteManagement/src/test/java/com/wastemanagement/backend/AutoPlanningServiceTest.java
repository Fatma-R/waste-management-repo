package com.wastemanagement.backend;

import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.model.tournee.auto.AutoMode;
import com.wastemanagement.backend.model.tournee.auto.BinSnapshot;
import com.wastemanagement.backend.service.tournee.TourneeService;
import com.wastemanagement.backend.service.tournee.auto.AutoPlanningConfigService;
import com.wastemanagement.backend.service.tournee.auto.AutoPlanningService;
import com.wastemanagement.backend.service.tournee.auto.BinSnapshotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoPlanningServiceTest {

    @Mock
    private AutoPlanningConfigService autoModeService;

    @Mock
    private BinSnapshotService binSnapshotService;

    @Mock
    private TourneeService tourneeService;

    @InjectMocks
    private AutoPlanningService autoPlanningService;

    @Test
    void runEmergencyLoop_offMode_doesNothing() {
        // given
        when(autoModeService.getAutoMode()).thenReturn(AutoMode.OFF);

        // when
        autoPlanningService.runEmergencyLoop();

        // then
        verifyNoInteractions(binSnapshotService, tourneeService);
    }

    @Test
    void runEmergencyLoop_emergencyMode_callsPlannerPerType_singleType() {
        // Simple case: only one type, 2 CPs
        when(autoModeService.getAutoMode()).thenReturn(AutoMode.EMERGENCIES_ONLY);

        BinSnapshot s1 = new BinSnapshot();
        s1.setTrashType(TrashType.PLASTIC);
        s1.setCollectionPointId("cp-1");

        BinSnapshot s2 = new BinSnapshot();
        s2.setTrashType(TrashType.PLASTIC);
        s2.setCollectionPointId("cp-2");

        when(binSnapshotService.getEmergencySnapshots())
                .thenReturn(List.of(s1, s2));

        when(tourneeService.planTourneesWithVroom(
                any(TrashType.class),
                anyDouble(),
                anySet()
        )).thenReturn(List.<TourneeResponseDTO>of());

        // when
        autoPlanningService.runEmergencyLoop();

        // then: called exactly once for PLASTIC with cp-1 & cp-2
        ArgumentCaptor<TrashType> typeCaptor = ArgumentCaptor.forClass(TrashType.class);
        ArgumentCaptor<Double> thresholdCaptor = ArgumentCaptor.forClass(Double.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> cpIdsCaptor = ArgumentCaptor.forClass(Set.class);

        verify(tourneeService, times(1))
                .planTourneesWithVroom(
                        typeCaptor.capture(),
                        thresholdCaptor.capture(),
                        cpIdsCaptor.capture()
                );

        assertThat(typeCaptor.getValue()).isEqualTo(TrashType.PLASTIC);
        assertThat(thresholdCaptor.getValue()).isEqualTo(0.0);
        assertThat(cpIdsCaptor.getValue())
                .containsExactlyInAnyOrder("cp-1", "cp-2");
    }

    @Test
    void runEmergencyLoop_emergencyMode_groupsByTypeAndDeduplicatesCpIds() {
        // given
        when(autoModeService.getAutoMode()).thenReturn(AutoMode.EMERGENCIES_ONLY);

        // PLASTIC: cp-1 (twice)
        BinSnapshot p1 = new BinSnapshot();
        p1.setTrashType(TrashType.PLASTIC);
        p1.setCollectionPointId("cp-1");

        BinSnapshot p1dup = new BinSnapshot();
        p1dup.setTrashType(TrashType.PLASTIC);
        p1dup.setCollectionPointId("cp-1");

        // ORGANIC: cp-2, cp-3
        BinSnapshot o1 = new BinSnapshot();
        o1.setTrashType(TrashType.ORGANIC);
        o1.setCollectionPointId("cp-2");

        BinSnapshot o2 = new BinSnapshot();
        o2.setTrashType(TrashType.ORGANIC);
        o2.setCollectionPointId("cp-3");

        when(binSnapshotService.getEmergencySnapshots())
                .thenReturn(List.of(p1, p1dup, o1, o2));

        when(tourneeService.planTourneesWithVroom(
                any(TrashType.class),
                anyDouble(),
                anySet()
        )).thenReturn(List.<TourneeResponseDTO>of());

        // when
        autoPlanningService.runEmergencyLoop();

        // then: called once for PLASTIC, once for ORGANIC
        ArgumentCaptor<TrashType> typeCaptor = ArgumentCaptor.forClass(TrashType.class);
        ArgumentCaptor<Double> thresholdCaptor = ArgumentCaptor.forClass(Double.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> cpIdsCaptor = ArgumentCaptor.forClass(Set.class);

        verify(tourneeService, times(2))
                .planTourneesWithVroom(
                        typeCaptor.capture(),
                        thresholdCaptor.capture(),
                        cpIdsCaptor.capture()
                );

        // We don't know the invocation order for sure, so we inspect all values
        List<TrashType> calledTypes = typeCaptor.getAllValues();
        List<Double> thresholds = thresholdCaptor.getAllValues();
        List<Set<String>> allCpSets = cpIdsCaptor.getAllValues();

        // both calls should use threshold 0.0
        assertThat(thresholds).containsExactly(0.0, 0.0);

        // Check that one call is PLASTIC with {cp-1}, other is ORGANIC with {cp-2,cp-3}
        boolean plasticOk = false;
        boolean organicOk = false;

        for (int i = 0; i < calledTypes.size(); i++) {
            TrashType t = calledTypes.get(i);
            Set<String> cps = allCpSets.get(i);

            if (t == TrashType.PLASTIC) {
                plasticOk = cps.size() == 1 && cps.contains("cp-1");
            } else if (t == TrashType.ORGANIC) {
                organicOk = cps.size() == 2
                        && cps.contains("cp-2")
                        && cps.contains("cp-3");
            }
        }

        assertThat(plasticOk).as("PLASTIC emergency CP set").isTrue();
        assertThat(organicOk).as("ORGANIC emergency CP set").isTrue();
    }

    @Test
    void runScheduledCycle_fullMode_callsMultiTypePlanner() {
        // given
        when(autoModeService.getAutoMode()).thenReturn(AutoMode.FULL);

        when(tourneeService.planTourneesWithVroom(anyList(), anyDouble()))
                .thenReturn(List.<TourneeResponseDTO>of());

        // when
        autoPlanningService.runScheduledCycle();

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TrashType>> typesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Double> thresholdCaptor = ArgumentCaptor.forClass(Double.class);

        verify(tourneeService, times(1))
                .planTourneesWithVroom(typesCaptor.capture(), thresholdCaptor.capture());

        List<TrashType> types = typesCaptor.getValue();
        assertThat(types).contains(
                TrashType.PLASTIC,
                TrashType.ORGANIC,
                TrashType.GLASS,
                TrashType.PAPER
        );
        assertThat(thresholdCaptor.getValue()).isEqualTo(80.0);
    }

    @Test
    void runScheduledCycle_nonFullMode_doesNothing() {
        // given
        when(autoModeService.getAutoMode()).thenReturn(AutoMode.EMERGENCIES_ONLY);

        // when
        autoPlanningService.runScheduledCycle();

        // then
        verifyNoInteractions(tourneeService);
    }
}
