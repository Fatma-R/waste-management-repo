package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;

import java.util.List;

public interface TourneeService {

    TourneeResponseDTO createTournee(TourneeRequestDTO dto);

    TourneeResponseDTO updateTournee(String id, TourneeRequestDTO dto);

    TourneeResponseDTO getTourneeById(String id);

    List<TourneeResponseDTO> getAllTournees();

    void deleteTournee(String id);
}
