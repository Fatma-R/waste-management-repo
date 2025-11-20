package com.wastemanagement.backend.repository.vehicle;

import com.wastemanagement.backend.model.vehicle.Vehicle;
import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    List<Vehicle> findByStatus(VehicleStatus status);
    List<Vehicle> findByFuelType(String fuelType);
}

