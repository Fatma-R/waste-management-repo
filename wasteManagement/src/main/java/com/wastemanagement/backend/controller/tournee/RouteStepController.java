package com.wastemanagement.backend.controller.tournee;

import com.wastemanagement.backend.dto.tournee.RouteStepRequestDTO;
import com.wastemanagement.backend.dto.tournee.RouteStepResponseDTO;
import com.wastemanagement.backend.mapper.tournee.RouteStepMapper;
import com.wastemanagement.backend.model.tournee.RouteStep;
import com.wastemanagement.backend.service.tournee.RouteStepService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/route-steps")
@RequiredArgsConstructor
public class RouteStepController {

    private final RouteStepService routeStepService;

    @PostMapping
    public RouteStepResponseDTO create(@RequestBody RouteStepRequestDTO dto) {
        RouteStep step = routeStepService.createRouteStep(dto);
        return RouteStepMapper.toResponse(step);
    }

    @GetMapping("/{id}")
    public RouteStepResponseDTO getById(@PathVariable String id) {
        RouteStep step = routeStepService.getRouteStepById(id);
        return RouteStepMapper.toResponse(step);
    }

    @GetMapping
    public List<RouteStepResponseDTO> getAll() {
        return routeStepService.getAllRouteSteps()
                .stream()
                .map(RouteStepMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public RouteStepResponseDTO update(@PathVariable String id, @RequestBody RouteStepRequestDTO dto) {
        RouteStep step = routeStepService.updateRouteStep(id, dto);
        return RouteStepMapper.toResponse(step);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        routeStepService.deleteRouteStep(id);
    }
}
