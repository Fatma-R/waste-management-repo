package com.wastemanagement.backend.repository;


import com.wastemanagement.backend.model.collection.CollectionPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionPointRepository extends MongoRepository<CollectionPoint, String> {

}
