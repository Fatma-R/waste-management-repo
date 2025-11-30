package com.wastemanagement.backend.repository;



import com.wastemanagement.backend.model.collection.Alert;
import com.wastemanagement.backend.model.collection.AlertType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface AlertRepository extends MongoRepository<Alert, String> {
    List<Alert> findByBinId(String binId);
    List<Alert> findByType(AlertType type);
    List<Alert> findByCleared(boolean cleared);
}
