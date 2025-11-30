package com.wastemanagement.backend.repository.collection;

import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.model.collection.TrashType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BinRepository extends MongoRepository<Bin, String> {
    List<Bin> findByActiveTrueAndType(TrashType type);
    
    /**
     * Find all bins associated with a specific collection point.
     * 
     * @param collectionPointId the collection point ID
     * @return list of bins for that collection point
     */
    List<Bin> findByCollectionPointId(String collectionPointId);
}
