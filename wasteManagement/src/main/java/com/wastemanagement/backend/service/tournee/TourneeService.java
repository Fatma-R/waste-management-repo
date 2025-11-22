package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.model.tournee.Tournee;

import java.util.List;

public interface TourneeService {

    Tournee createTournee(TourneeRequestDTO dto);

    Tournee updateTournee(String id, TourneeRequestDTO dto);

    Tournee getTourneeById(String id);

    List<Tournee> getAllTournees();

    void deleteTournee(String id);
}
