package com.wastemanagement.backend.repository.tournee;

import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.model.tournee.TourneeStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TourneeRepository extends MongoRepository<Tournee, String> {
    List<Tournee> findByTourneeTypeAndStatus(TrashType type, TourneeStatus status);
    List<Tournee> findByStatusAndIdIn(TourneeStatus status, Collection<String> ids);
    List<Tournee> findByStatus(TourneeStatus status);

}
