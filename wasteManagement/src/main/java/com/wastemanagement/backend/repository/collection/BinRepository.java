package com.wastemanagement.backend.repository.collection;

import com.wastemanagement.backend.model.collection.Bin;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BinRepository extends MongoRepository<Bin, String> {
}
