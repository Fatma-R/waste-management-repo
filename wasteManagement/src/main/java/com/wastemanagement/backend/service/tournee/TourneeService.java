package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.model.tournee.TourneeStatus;

import java.util.List;
import java.util.Set;

public interface TourneeService {

    TourneeResponseDTO createTournee(TourneeRequestDTO dto);

    TourneeResponseDTO updateTournee(String id, TourneeRequestDTO dto);

    TourneeResponseDTO getTourneeById(String id);

    List<TourneeResponseDTO> getAllTournees();

    void deleteTournee(String id);

    List<TourneeResponseDTO> planTourneesWithVroom(TrashType type, double fillThreshold);

    List<TourneeResponseDTO> planTourneesWithVroom(List<TrashType> type, double fillThreshold);

    List<TourneeResponseDTO> planTourneesWithVroom(TrashType type,
                                                   double fillThreshold,
                                                   Set<String> forcedCollectionPointIds);

    List<TourneeResponseDTO> findByStatus(TourneeStatus status);
}
