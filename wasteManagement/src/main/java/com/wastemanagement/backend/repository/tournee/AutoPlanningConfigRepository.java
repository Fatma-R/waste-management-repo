package com.wastemanagement.backend.repository.tournee;

import com.wastemanagement.backend.model.tournee.auto.AutoPlanningConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AutoPlanningConfigRepository extends MongoRepository<AutoPlanningConfig, String> {
}
