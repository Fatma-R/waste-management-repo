package com.wastemanagement.backend.repository.collection;

import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.model.collection.TrashType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BinRepository extends MongoRepository<Bin, String> {
    List<Bin> findByActiveTrueAndType(TrashType type);
}
