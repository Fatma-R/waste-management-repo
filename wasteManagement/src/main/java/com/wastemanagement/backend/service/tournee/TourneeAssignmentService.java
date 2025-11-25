package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeAssignmentRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeAssignmentResponseDTO;

import java.util.List;
import java.util.Optional;

public interface TourneeAssignmentService {

    List<TourneeAssignmentResponseDTO> getAll();

    Optional<TourneeAssignmentResponseDTO> getById(String id);

    TourneeAssignmentResponseDTO create(TourneeAssignmentRequestDTO dto);

    Optional<TourneeAssignmentResponseDTO> update(String id, TourneeAssignmentRequestDTO dto);

    boolean delete(String id);
}
