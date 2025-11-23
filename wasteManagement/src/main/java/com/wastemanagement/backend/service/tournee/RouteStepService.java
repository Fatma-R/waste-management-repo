package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.RouteStepRequestDTO;
import com.wastemanagement.backend.dto.tournee.RouteStepResponseDTO;

import java.util.List;

public interface RouteStepService {

    RouteStepResponseDTO createRouteStep(RouteStepRequestDTO dto);

    RouteStepResponseDTO getRouteStepById(String id);

    List<RouteStepResponseDTO> getAllRouteSteps();

    RouteStepResponseDTO updateRouteStep(String id, RouteStepRequestDTO dto);

    void deleteRouteStep(String id);
}
