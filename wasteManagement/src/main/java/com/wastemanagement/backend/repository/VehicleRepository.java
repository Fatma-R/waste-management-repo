package com.wastemanagement.backend.repository;

import com.wastemanagement.backend.model.vehicle.FuelType;
import com.wastemanagement.backend.model.vehicle.Vehicle;
import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    List<Vehicle> findByStatus(VehicleStatus status);
    List<Vehicle> findByStatusAndBusyFalse(VehicleStatus status);
    List<Vehicle> findByFuelType(FuelType fuelType);
    Optional<Vehicle> findFirstByStatus(VehicleStatus status);
    Optional<Vehicle> findFirstByStatusAndBusyFalse(VehicleStatus status);
}
