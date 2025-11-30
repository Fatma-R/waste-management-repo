package com.wastemanagement.backend;

import com.wastemanagement.backend.dto.GeoJSONPointDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentRequestDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentResponseDTO;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.model.collection.incident.Incident;
import com.wastemanagement.backend.model.collection.incident.IncidentSeverity;
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
import org.springframework.data.mongodb.core.index.GeospatialIndex;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for incident impact propagation with real MongoDB.
 *
 * Uses Spring's embedded MongoDB to exercise real geospatial queries while testing the service layer.
 */
@SpringBootTest
class IncidentImpactPropagationIntegrationTest {

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
        incidentRepository.deleteAll();
        collectionPointRepository.deleteAll();
        binRepository.deleteAll();
        IndexOperations indexOps = mongoTemplate.indexOps(CollectionPoint.class);
        indexOps.createIndex(
                new GeospatialIndex("location")
                        .typed(GeoSpatialIndexType.GEO_2DSPHERE)
        );


        // Incident location: 10.5E, 45.5N (Milan area)
        incidentLocation = new GeoJSONPoint(10.5, 45.5);
    }

    /**
     * Test: Incident within 500m radius deactivates nearby collection points' bins
     *
     * Scenario:
     * - Incident at (10.5, 45.5)
     * - CP1 at (10.50036, 45.50036) ~5m away - WITHIN radius
     * - CP2 at (10.50900, 45.50900) ~1.4km away - OUTSIDE radius
     *
     * Expected: Only CP1's bins are deactivated
     */
    @Test
    void testIncidentDeactivatesBinsWithinGeospatialRadius() {
        IncidentRequestDTO incidentDTO = new IncidentRequestDTO();
        incidentDTO.setDescription("Traffic incident");
        incidentDTO.setSeverity(IncidentSeverity.HIGH);
        GeoJSONPointDTO incidentPoint = new GeoJSONPointDTO();
        incidentPoint.setCoordinates(new double[]{incidentLocation.getCoordinates()[0], incidentLocation.getCoordinates()[1]});
        incidentDTO.setLocation(incidentPoint);

        CollectionPoint cpClose = new CollectionPoint();
        cpClose.setLocation(new GeoJSONPoint(10.50036, 45.50036));
        cpClose.setActive(true);
        cpClose.setAdresse("Close Address");
        collectionPointRepository.save(cpClose);

        CollectionPoint cpFar = new CollectionPoint();
        cpFar.setLocation(new GeoJSONPoint(10.50900, 45.50900));
        cpFar.setActive(true);
        cpFar.setAdresse("Far Address");
        collectionPointRepository.save(cpFar);

        Bin bin1 = new Bin();
        bin1.setActive(true);
        bin1.setCollectionPointId(cpClose.getId());
        binRepository.save(bin1);

        Bin bin2 = new Bin();
        bin2.setActive(true);
        bin2.setCollectionPointId(cpFar.getId());
        binRepository.save(bin2);

        IncidentResponseDTO result = incidentService.createIncident(incidentDTO);

        assertNotNull(result);

        Incident savedIncident = incidentRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedIncident);

        Bin savedBin1 = binRepository.findById(bin1.getId()).orElse(null);
        Bin savedBin2 = binRepository.findById(bin2.getId()).orElse(null);

        assertNotNull(savedBin1);
        assertNotNull(savedBin2);

        assertFalse(savedBin1.isActive(), "Bin in close collection point should be deactivated");
        assertTrue(savedBin2.isActive(), "Bin in far collection point should remain active");
    }

    /**
     * Test: Multiple collection points within radius all get their bins deactivated
     *
     * Scenario:
     * - Incident at (10.5, 45.5)
     * - CP1 at (10.50036, 45.50036) ~5m - WITHIN
     * - CP2 at (10.50045, 45.50045) ~7m - WITHIN
     * - CP3 at (10.50090, 45.50090) ~1.4km - OUTSIDE
     *
     * Expected: CP1 and CP2's bins deactivated, CP3's bins remain active
     */
    @Test
    void testMultipleCollectionPointsWithinRadiusAllGetDeactivated() {
        IncidentRequestDTO incidentDTO = new IncidentRequestDTO();
        incidentDTO.setDescription("Large scale incident");
        incidentDTO.setSeverity(IncidentSeverity.CRITICAL);
        GeoJSONPointDTO incidentPoint = new GeoJSONPointDTO();
        incidentPoint.setCoordinates(new double[]{incidentLocation.getCoordinates()[0], incidentLocation.getCoordinates()[1]});
        incidentDTO.setLocation(incidentPoint);

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

        IncidentResponseDTO result = incidentService.createIncident(incidentDTO);

        assertNotNull(result);

        Bin savedBin1 = binRepository.findById(bin1.getId()).orElse(null);
        Bin savedBin2 = binRepository.findById(bin2.getId()).orElse(null);
        Bin savedBin3 = binRepository.findById(bin3.getId()).orElse(null);

        assertFalse(savedBin1.isActive(), "Bin1 in close CP1 should be deactivated");
        assertFalse(savedBin2.isActive(), "Bin2 in close CP2 should be deactivated");
        assertTrue(savedBin3.isActive(), "Bin3 in far CP3 should remain active");
    }

    /**
     * Test: Geospatial query correctly excludes collection points outside radius
     *
     * Scenario:
     * - Incident at (10.5, 45.5)
     * - CP1 at exactly (10.5, 45.5) - 0m - WITHIN
     * - CP2 at (10.5, 45.504) ~440m - WITHIN
     * - CP3 at (10.5, 45.510) ~1.1km - OUTSIDE (500m radius)
     *
     * Expected: Only CP1 and CP2 collection points found by geospatial query
     */
    @Test
    void testGeospatialQueryExcludesPointsOutsideRadius() {
        IncidentRequestDTO incidentDTO = new IncidentRequestDTO();
        incidentDTO.setDescription("Test");
        incidentDTO.setSeverity(IncidentSeverity.HIGH);
        GeoJSONPointDTO incidentPoint = new GeoJSONPointDTO();
        incidentPoint.setCoordinates(new double[]{incidentLocation.getCoordinates()[0], incidentLocation.getCoordinates()[1]});
        incidentDTO.setLocation(incidentPoint);

        CollectionPoint cpExact = new CollectionPoint();
        cpExact.setLocation(new GeoJSONPoint(10.5, 45.5));
        cpExact.setActive(true);
        cpExact.setAdresse("Exact");
        collectionPointRepository.save(cpExact);

        CollectionPoint cpNear = new CollectionPoint();
        cpNear.setLocation(new GeoJSONPoint(10.5, 45.504)); // ~440m away, within 500m radius
        cpNear.setActive(true);
        cpNear.setAdresse("Near");
        collectionPointRepository.save(cpNear);

        CollectionPoint cpFar = new CollectionPoint();
        cpFar.setLocation(new GeoJSONPoint(10.5, 45.510));
        cpFar.setActive(true);
        cpFar.setAdresse("Far");
        collectionPointRepository.save(cpFar);

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

        IncidentResponseDTO result = incidentService.createIncident(incidentDTO);

        assertNotNull(result);

        List<CollectionPoint> nearby = collectionPointRepository.findNearby(incidentLocation, 500);
        assertEquals(2, nearby.size(), "Should find exactly 2 collection points within 500m");

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
        IncidentRequestDTO incidentDTO = new IncidentRequestDTO();
        incidentDTO.setDescription("Test");
        incidentDTO.setSeverity(IncidentSeverity.HIGH);
        GeoJSONPointDTO incidentPoint = new GeoJSONPointDTO();
        incidentPoint.setCoordinates(new double[]{incidentLocation.getCoordinates()[0], incidentLocation.getCoordinates()[1]});
        incidentDTO.setLocation(incidentPoint);

        IncidentResponseDTO result = incidentService.createIncident(incidentDTO);

        assertNotNull(result);

        Incident savedIncident = incidentRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedIncident);

        List<Bin> allBins = binRepository.findAll();
        assertTrue(allBins.isEmpty(), "No bins should exist");
    }

    /**
     * Test: Verify geospatial index is properly created
     * Expected: findNearby() query works correctly with the index
     */
    @Test
    void testGeospatialIndexFunctionality() {
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

        List<CollectionPoint> nearby = collectionPointRepository.findNearby(
                new GeoJSONPoint(10.5, 45.5), 700
        );

        assertNotNull(nearby);
        assertEquals(2, nearby.size(), "Should find both collection points");
        assertTrue(nearby.stream().anyMatch(cp -> cp.getId().equals(cp1.getId())));
        assertTrue(nearby.stream().anyMatch(cp -> cp.getId().equals(cp2.getId())));
    }
}
