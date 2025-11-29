package com.wastemanagement.backend.repository.tournee;

import com.wastemanagement.backend.model.tournee.TourneeAssignment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourneeAssignmentRepository extends MongoRepository<TourneeAssignment, String> {
}
