package com.wastemanagement.backend.repository.tournee;

import com.wastemanagement.backend.model.tournee.Tournee;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourneeRepository extends CrudRepository<Tournee, String> {
}
