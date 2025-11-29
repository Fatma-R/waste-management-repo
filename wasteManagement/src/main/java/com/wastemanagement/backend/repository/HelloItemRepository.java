package com.wastemanagement.backend.repository;

import com.wastemanagement.backend.model.HelloItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HelloItemRepository extends MongoRepository<HelloItem, String> {

}
