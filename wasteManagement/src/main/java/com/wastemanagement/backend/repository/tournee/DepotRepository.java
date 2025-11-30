package com.wastemanagement.backend.repository.tournee;

import com.wastemanagement.backend.model.tournee.Depot;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepotRepository extends CrudRepository<Depot, String> {
}
