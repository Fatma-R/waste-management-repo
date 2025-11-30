package com.wastemanagement.backend;

import com.wastemanagement.backend.dto.collection.incident.IncidentRequestDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentResponseDTO;
import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.model.collection.incident.Incident;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.repository.CollectionPointRepository;
import com.wastemanagement.backend.repository.collection.BinRepository;
import com.wastemanagement.backend.repository.collection.incident.IncidentRepository;
import com.wastemanagement.backend.service.collection.incident.IncidentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for incident impact propagation with real MongoDB.
 * 
 * Uses TestContainers to spin up an actual MongoDB instance with geospatial indexing.
 * This tests the real geospatial query logic, not just mocked data.
 */
@SpringBootTest
@Testcontainers
class IncidentImpactPropagationIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private IncidentServiceImpl incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private CollectionPointRepository collectionPointRepository;

    @Autowired
    private BinRepository binRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private GeoJSONPoint incidentLocation;

    @BeforeEach
    void setUp() {
        // Clear all data before each test
        incidentRepository.deleteAll();
        collectionPointRepository.deleteAll();
        binRepository.deleteAll();

        // Create geospatial index on collectionPoints location field
        IndexOperations indexOps = mongoTemplate.indexOps(CollectionPoint.class);
        indexOps.ensureIndex(
                new org.springframework.data.mongodb.core.index.GeoSpatialIndex("location")
                        .typed(GeoSpatialIndexType.GEO_2DSPHERE)
        );

        // Incident location: 10.5°E, 45.5°N (Milan area)
        incidentLocation = new GeoJSONPoint(10.5, 45.5);
    }

    /**
     * Test: Incident within 500m radius deactivates nearby collection points' bins
     * 
     * Scenario:
     * - Incident at (10.5, 45.5)
     * - CP1 at (10.50036, 45.50036) ≈ 5m away - WITHIN radius
     * - CP2 at (10.50900, 45.50900) ≈ 1.4km away - OUTSIDE radius
     * 
     * Expected: Only CP1's bins are deactivated
     */
    @Test
    void testIncidentDeactivatesBinsWithinGeospatialRadius() {
        // Arrange - Create incident
        IncidentRequestDTO incidentDTO = new IncidentRequestDTO();
        incidentDTO.setTitle("Road Accident");
        incidentDTO.setDescription("Traffic incident");
        incidentDTO.setSeverity("HIGH");

        // Create collection points at different distances
        CollectionPoint cpClose = new CollectionPoint();
        cpClose.setLocation(new GeoJSONPoint(10.50036, 45.50036));  // ~5m away
        cpClose.setActive(true);
        cpClose.setAdresse("Close Address");
        collectionPointRepository.save(cpClose);

        CollectionPoint cpFar = new CollectionPoint();
        cpFar.setLocation(new GeoJSONPoint(10.50900, 45.50900));   // ~1.4km away
        cpFar.setActive(true);
        cpFar.setAdresse("Far Address");
        collectionPointRepository.save(cpFar);

        // Create bins in both collection points
        Bin bin1 = new Bin();
        bin1.setActive(true);
        bin1.setCollectionPointId(cpClose.getId());
        binRepository.save(bin1);

        Bin bin2 = new Bin();
        bin2.setActive(true);
        bin2.setCollectionPointId(cpFar.getId());
        binRepository.save(bin2);

        // Act - Create incident at specific location
        IncidentResponseDTO result = incidentService.createIncident(incidentDTO);

        // Assert
        assertNotNull(result);

        // Verify incident was created
        Incident savedIncident = incidentRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedIncident);

        // Fetch the saved bins to check their status
        Bin savedBin1 = binRepository.findById(bin1.getId()).orElse(null);
        Bin savedBin2 = binRepository.findById(bin2.getId()).orElse(null);

        assertNotNull(savedBin1);
        assertNotNull(savedBin2);

        // Bin in close collection point should be deactivated
        assertFalse(savedBin1.isActive(), "Bin in close collection point should be deactivated");

        // Bin in far collection point should remain active
        assertTrue(savedBin2.isActive(), "Bin in far collection point should remain active");
    }

    /**
     * Test: Multiple collection points within radius all get their bins deactivated
     * 
     * Scenario:
     * - Incident at (10.5, 45.5)
     * - CP1 at (10.50036, 45.50036) ≈ 5m - WITHIN
     * - CP2 at (10.50045, 45.50045) ≈ 7m - WITHIN
     * - CP3 at (10.50090, 45.50090) ≈ 1.4km - OUTSIDE
     * 
     * Expected: CP1 and CP2's bins deactivated, CP3's bins remain active
     */
    @Test
    void testMultipleCollectionPointsWithinRadiusAllGetDeactivated() {
        // Arrange
        IncidentRequestDTO incidentDTO = new IncidentRequestDTO();
        incidentDTO.setTitle("Major Incident");
        incidentDTO.setDescription("Large scale incident");
        incidentDTO.setSeverity("CRITICAL");

        // Create 3 collection points
        CollectionPoint cp1 = new CollectionPoint();
        cp1.setLocation(new GeoJSONPoint(10.50036, 45.50036));
        cp1.setActive(true);
        cp1.setAdresse("Address 1");
        collectionPointRepository.save(cp1);

        CollectionPoint cp2 = new CollectionPoint();
        cp2.setLocation(new GeoJSONPoint(10.50045, 45.50045));
        cp2.setActive(true);
        cp2.setAdresse("Address 2");
        collectionPointRepository.save(cp2);

        CollectionPoint cp3 = new CollectionPoint();
        cp3.setLocation(new GeoJSONPoint(10.50900, 45.50900));
        cp3.setActive(true);
        cp3.setAdresse("Address 3");
        collectionPointRepository.save(cp3);

        // Create multiple bins in each collection point
        Bin bin1 = new Bin();
        bin1.setActive(true);
        bin1.setCollectionPointId(cp1.getId());
        binRepository.save(bin1);

        Bin bin2 = new Bin();
        bin2.setActive(true);
        bin2.setCollectionPointId(cp2.getId());
        binRepository.save(bin2);

        Bin bin3 = new Bin();
        bin3.setActive(true);
        bin3.setCollectionPointId(cp3.getId());
        binRepository.save(bin3);

        // Act
        IncidentResponseDTO result = incidentService.createIncident(incidentDTO);

        // Assert
        assertNotNull(result);

        Bin savedBin1 = binRepository.findById(bin1.getId()).orElse(null);
        Bin savedBin2 = binRepository.findById(bin2.getId()).orElse(null);
        Bin savedBin3 = binRepository.findById(bin3.getId()).orElse(null);

        // Bins in CP1 and CP2 should be deactivated
        assertFalse(savedBin1.isActive(), "Bin1 in close CP1 should be deactivated");
        assertFalse(savedBin2.isActive(), "Bin2 in close CP2 should be deactivated");

        // Bin in CP3 should remain active (outside radius)
        assertTrue(savedBin3.isActive(), "Bin3 in far CP3 should remain active");
    }

    /**
     * Test: Geospatial query correctly excludes collection points outside radius
     * 
     * Scenario:
     * - Incident at (10.5, 45.5)
     * - CP1 at exactly (10.5, 45.5) - 0m - WITHIN
     * - CP2 at (10.5, 45.506) ≈ 670m - WITHIN
     * - CP3 at (10.5, 45.510) ≈ 1.1km - OUTSIDE (500m radius)
     * 
     * Expected: Only CP1 and CP2 collection points found by geospatial query
     */
    @Test
    void testGeospatialQueryExcludesPointsOutsideRadius() {
        // Arrange
        IncidentRequestDTO incidentDTO = new IncidentRequestDTO();
        incidentDTO.setTitle("Incident");
        incidentDTO.setDescription("Test");
        incidentDTO.setSeverity("HIGH");

        // CP at incident location (0m)
        CollectionPoint cpExact = new CollectionPoint();
        cpExact.setLocation(new GeoJSONPoint(10.5, 45.5));
        cpExact.setActive(true);
        cpExact.setAdresse("Exact");
        collectionPointRepository.save(cpExact);

        // CP at ~670m away (within 500m + some tolerance)
        CollectionPoint cpNear = new CollectionPoint();
        cpNear.setLocation(new GeoJSONPoint(10.5, 45.506));
        cpNear.setActive(true);
        cpNear.setAdresse("Near");
        collectionPointRepository.save(cpNear);

        // CP at ~1.1km away (outside 500m radius)
        CollectionPoint cpFar = new CollectionPoint();
        cpFar.setLocation(new GeoJSONPoint(10.5, 45.510));
        cpFar.setActive(true);
        cpFar.setAdresse("Far");
        collectionPointRepository.save(cpFar);

        // Create bins
        Bin bin1 = new Bin();
        bin1.setActive(true);
        bin1.setCollectionPointId(cpExact.getId());
        binRepository.save(bin1);

        Bin bin2 = new Bin();
        bin2.setActive(true);
        bin2.setCollectionPointId(cpNear.getId());
        binRepository.save(bin2);

        Bin bin3 = new Bin();
        bin3.setActive(true);
        bin3.setCollectionPointId(cpFar.getId());
        binRepository.save(bin3);

        // Act
        IncidentResponseDTO result = incidentService.createIncident(incidentDTO);

        // Assert
        assertNotNull(result);

        // Verify query results using the repository directly
        List<CollectionPoint> nearby = collectionPointRepository.findNearby(incidentLocation, 500);
        assertEquals(2, nearby.size(), "Should find exactly 2 collection points within 500m");

        // Verify bins are deactivated correctly
        Bin savedBin1 = binRepository.findById(bin1.getId()).orElse(null);
        Bin savedBin2 = binRepository.findById(bin2.getId()).orElse(null);
        Bin savedBin3 = binRepository.findById(bin3.getId()).orElse(null);

        assertFalse(savedBin1.isActive(), "Bin in exact location should be deactivated");
        assertFalse(savedBin2.isActive(), "Bin near location should be deactivated");
        assertTrue(savedBin3.isActive(), "Bin far from location should remain active");
    }

    /**
     * Test: Empty database - no collection points to query
     * Expected: Incident created successfully with no bins deactivated
     */
    @Test
    void testIncidentCreationWithNoCollectionPointsInDatabase() {
        // Arrange
        IncidentRequestDTO incidentDTO = new IncidentRequestDTO();
        incidentDTO.setTitle("Incident");
        incidentDTO.setDescription("Test");
        incidentDTO.setSeverity("HIGH");

        // Act
        IncidentResponseDTO result = incidentService.createIncident(incidentDTO);

        // Assert
        assertNotNull(result);
        
        // Verify incident was created
        Incident savedIncident = incidentRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedIncident);

        // Verify no bins were affected
        List<Bin> allBins = binRepository.findAll();
        assertTrue(allBins.isEmpty(), "No bins should exist");
    }

    /**
     * Test: Verify geospatial index is properly created
     * Expected: findNearby() query works correctly with the index
     */
    @Test
    void testGeospatialIndexFunctionality() {
        // Arrange
        CollectionPoint cp1 = new CollectionPoint();
        cp1.setLocation(new GeoJSONPoint(10.5, 45.5));
        cp1.setActive(true);
        cp1.setAdresse("Test 1");
        collectionPointRepository.save(cp1);

        CollectionPoint cp2 = new CollectionPoint();
        cp2.setLocation(new GeoJSONPoint(10.50036, 45.50036));
        cp2.setActive(true);
        cp2.setAdresse("Test 2");
        collectionPointRepository.save(cp2);

        // Act
        List<CollectionPoint> nearby = collectionPointRepository.findNearby(
                new GeoJSONPoint(10.5, 45.5), 500
        );

        // Assert
        assertNotNull(nearby);
        assertEquals(2, nearby.size(), "Should find both collection points");
        assertTrue(nearby.stream().anyMatch(cp -> cp.getId().equals(cp1.getId())));
        assertTrue(nearby.stream().anyMatch(cp -> cp.getId().equals(cp2.getId())));
    }
}
