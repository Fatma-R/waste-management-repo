package com.wastemanagement.backend.repository.tournee;

import com.wastemanagement.backend.model.tournee.Tournee;
import org.springframework.data.repository.CrudRepository;

public interface TourneeRepository extends CrudRepository<Tournee, String> {
}
