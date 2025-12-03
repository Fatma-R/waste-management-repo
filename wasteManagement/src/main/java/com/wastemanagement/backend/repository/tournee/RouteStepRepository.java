package com.wastemanagement.backend.repository.tournee;

import com.wastemanagement.backend.model.tournee.RouteStep;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteStepRepository extends CrudRepository<RouteStep, String> {
}
