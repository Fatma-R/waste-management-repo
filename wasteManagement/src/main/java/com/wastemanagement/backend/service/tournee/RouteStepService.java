package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.RouteStepRequestDTO;
import com.wastemanagement.backend.model.tournee.RouteStep;

import java.util.List;

public interface RouteStepService {

    RouteStep createRouteStep(RouteStepRequestDTO dto);

    RouteStep getRouteStepById(String id);

    List<RouteStep> getAllRouteSteps();

    RouteStep updateRouteStep(String id, RouteStepRequestDTO dto);

    void deleteRouteStep(String id);
}
