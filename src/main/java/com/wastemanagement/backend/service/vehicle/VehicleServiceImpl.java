package com.wastemanagement.backend.service.vehicle;

import com.wastemanagement.backend.dto.vehicle.VehicleRequestDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleResponseDTO;
import com.wastemanagement.backend.mapper.vehicle.VehicleMapper;
import com.wastemanagement.backend.model.vehicle.Vehicle;
import com.wastemanagement.backend.repository.vehicle.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private VehicleRepository repository;

    @Autowired
    private VehicleMapper mapper;

    @Override
    public VehicleResponseDTO createVehicle(VehicleRequestDTO dto) {
        Vehicle vehicle = mapper.toEntity(dto);
        Vehicle saved = repository.save(vehicle);
        return mapper.toResponseDTO(saved);
    }

    @Override
    public List<VehicleResponseDTO> getAllVehicles(int page, int size) {
        return repository.findAll(PageRequest.of(page, size))
                .stream().map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public VehicleResponseDTO getVehicleById(String id) {
        Optional<Vehicle> vehicle = repository.findById(id);
        return vehicle.map(mapper::toResponseDTO).orElse(null);
    }

    @Override
    public VehicleResponseDTO updateVehicle(String id, VehicleRequestDTO dto) {
        Optional<Vehicle> optionalVehicle = repository.findById(id);
        if (!optionalVehicle.isPresent()) return null;

        Vehicle vehicle = optionalVehicle.get();
        mapper.updateEntity(dto, vehicle);
        return mapper.toResponseDTO(repository.save(vehicle));
    }

    @Override
    public boolean deleteVehicle(String id) {
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }

    @Override
    public List<VehicleResponseDTO> getVehiclesByStatus(String status) {
        return repository.findByStatus(Enum.valueOf(com.wastemanagement.backend.model.vehicle.VehicleStatus.class, status))
                .stream().map(mapper::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<VehicleResponseDTO> getVehiclesByFuelType(String fuelType) {
        return repository.findByFuelType(fuelType)
                .stream().map(mapper::toResponseDTO).collect(Collectors.toList());
    }
}

