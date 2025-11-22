package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.RouteStepRequestDTO;
import com.wastemanagement.backend.mapper.tournee.RouteStepMapper;
import com.wastemanagement.backend.model.tournee.RouteStep;
import com.wastemanagement.backend.repository.tournee.RouteStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteStepServiceImpl implements RouteStepService {

    private final RouteStepRepository routeStepRepository;

    @Override
    public RouteStep createRouteStep(RouteStepRequestDTO dto) {
        RouteStep step = RouteStepMapper.toEntity(dto);
        return routeStepRepository.save(step);
    }

    @Override
    public RouteStep getRouteStepById(String id) {
        return routeStepRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RouteStep not found"));
    }

    @Override
    public List<RouteStep> getAllRouteSteps() {
        return (List<RouteStep>) routeStepRepository.findAll();
    }

    @Override
    public RouteStep updateRouteStep(String id, RouteStepRequestDTO dto) {
        RouteStep existing = getRouteStepById(id);
        RouteStepMapper.updateEntity(existing, dto);
        return routeStepRepository.save(existing);
    }

    @Override
    public void deleteRouteStep(String id) {
        routeStepRepository.deleteById(id);
    }
}
