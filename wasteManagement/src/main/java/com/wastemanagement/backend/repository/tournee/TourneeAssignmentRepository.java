package com.wastemanagement.backend.repository.tournee;

import com.wastemanagement.backend.model.tournee.TourneeAssignment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TourneeAssignmentRepository extends MongoRepository<TourneeAssignment, String> {
    List<TourneeAssignment> findByEmployeeId(String employeeId);

    List<TourneeAssignment> findByTourneeId(String tourneeId);
}
