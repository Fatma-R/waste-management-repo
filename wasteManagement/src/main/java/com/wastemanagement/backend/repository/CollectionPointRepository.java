package com.wastemanagement.backend.repository;


import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.model.GeoJSONPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionPointRepository extends MongoRepository<CollectionPoint, String> {

    @Query("""
{
  'location': {
    $nearSphere: {
      $geometry: ?0,
      $maxDistance: ?1
    }   
  }
}
""")
    List<CollectionPoint> findNearby(GeoJSONPoint location, long radiusInMeters);

}
