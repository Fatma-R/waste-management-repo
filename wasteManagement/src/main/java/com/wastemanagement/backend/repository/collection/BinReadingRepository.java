package com.wastemanagement.backend.repository.collection;

import com.wastemanagement.backend.model.collection.BinReading;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BinReadingRepository extends MongoRepository<BinReading, String> {

    // Pour récupérer toutes les lectures d’une poubelle spécifique
    List<BinReading> findByBinId(String binId);

    // Tu peux ajouter des recherches par date ou par plage
    List<BinReading> findByBinIdAndTsBetween(String binId, java.util.Date start, java.util.Date end);

    BinReading findTopByBinIdOrderByTsDesc(String binId);
}
