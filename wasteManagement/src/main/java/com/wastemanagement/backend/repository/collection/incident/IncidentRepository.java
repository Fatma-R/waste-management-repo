package com.wastemanagement.backend.repository.collection.incident;

import com.wastemanagement.backend.model.collection.incident.Incident;
import com.wastemanagement.backend.model.collection.incident.IncidentType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface IncidentRepository extends CrudRepository<Incident, String> {
    @Override
    List<Incident> findAll(); // Override to return List instead of Iterable. if u switch to MongoRepository, this isn't needed
    List<Incident> findByType(IncidentType type);
    List<Incident> findByStatus(String status);
}
