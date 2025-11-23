package com.wastemanagement.backend.controller.tournee;

import com.wastemanagement.backend.dto.tournee.RouteStepRequestDTO;
import com.wastemanagement.backend.dto.tournee.RouteStepResponseDTO;
import com.wastemanagement.backend.service.tournee.RouteStepService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/route-steps")
@RequiredArgsConstructor
public class RouteStepController {

    private final RouteStepService routeStepService;

    @PostMapping
    public RouteStepResponseDTO create(@RequestBody RouteStepRequestDTO dto) {
        return routeStepService.createRouteStep(dto);
    }

    @GetMapping("/{id}")
    public RouteStepResponseDTO getById(@PathVariable String id) {
        return routeStepService.getRouteStepById(id);
    }

    @GetMapping
    public List<RouteStepResponseDTO> getAll() {
        return routeStepService.getAllRouteSteps();
    }

    @PutMapping("/{id}")
    public RouteStepResponseDTO update(@PathVariable String id,
                                       @RequestBody RouteStepRequestDTO dto) {
        return routeStepService.updateRouteStep(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        routeStepService.deleteRouteStep(id);
    }
}
