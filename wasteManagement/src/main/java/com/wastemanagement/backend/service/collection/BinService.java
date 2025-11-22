package com.wastemanagement.backend.service.collection;


import com.wastemanagement.backend.model.collection.Bin;

import java.util.List;
import java.util.Optional;

public interface BinService {
    List<Bin> getAllBins();
    Optional<Bin> getBinById(String id);
    Bin createBin(Bin bin);
    Optional<Bin> updateBin(String id, Bin bin);
    boolean deleteBin(String id);
}
