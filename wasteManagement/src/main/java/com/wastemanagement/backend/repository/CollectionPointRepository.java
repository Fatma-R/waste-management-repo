package com.wastemanagement.backend.repository;


import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.model.GeoJSONPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionPointRepository extends MongoRepository<CollectionPoint, String> {
    
    /**
     * Find all collection points within a given radius from a center location.
     * Uses MongoDB's geospatial $near operator with maxDistance in meters.
     * 
     * @param location the center point (GeoJSONPoint)
     * @param radiusInMeters the search radius in meters
     * @return list of collection points within the radius
     */
    @Query("{ 'location': { $near: { $geometry: ?0, $maxDistance: ?1 } } }")
    List<CollectionPoint> findNearby(GeoJSONPoint location, long radiusInMeters);
}
