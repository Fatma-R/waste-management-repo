package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.RouteStepRequestDTO;
import com.wastemanagement.backend.dto.tournee.RouteStepResponseDTO;
import com.wastemanagement.backend.mapper.tournee.RouteStepMapper;
import com.wastemanagement.backend.model.tournee.RouteStep;
import com.wastemanagement.backend.repository.tournee.RouteStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteStepServiceImpl implements RouteStepService {

    private final RouteStepRepository routeStepRepository;

    @Override
    public RouteStepResponseDTO createRouteStep(RouteStepRequestDTO dto) {
        RouteStep step = RouteStepMapper.toEntity(dto);
        RouteStep saved = routeStepRepository.save(step);
        return RouteStepMapper.toResponse(saved);
    }

    @Override
    public RouteStepResponseDTO getRouteStepById(String id) {
        RouteStep step = routeStepRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RouteStep not found"));
        return RouteStepMapper.toResponse(step);
    }

    @Override
    public List<RouteStepResponseDTO> getAllRouteSteps() {
        return ((List<RouteStep>) routeStepRepository.findAll())
                .stream()
                .map(RouteStepMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RouteStepResponseDTO updateRouteStep(String id, RouteStepRequestDTO dto) {
        RouteStep existing = routeStepRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RouteStep not found"));
        RouteStepMapper.updateEntity(existing, dto);
        RouteStep saved = routeStepRepository.save(existing);
        return RouteStepMapper.toResponse(saved);
    }

    @Override
    public void deleteRouteStep(String id) {
        routeStepRepository.deleteById(id);
    }
}
