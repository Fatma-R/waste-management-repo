package com.wastemanagement.backend.service.collection;

import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.repository.collection.BinRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BinServiceImpl implements BinService {

    private final BinRepository binRepository;

    public BinServiceImpl(BinRepository binRepository) {
        this.binRepository = binRepository;
    }

    @Override
    public List<Bin> getAllBins() {
        return binRepository.findAll();
    }

    @Override
    public Optional<Bin> getBinById(String id) {
        return binRepository.findById(id);
    }

    @Override
    public Bin createBin(Bin bin) {
        return binRepository.save(bin);
    }

    @Override
    public Optional<Bin> updateBin(String id, Bin updatedBin) {
        return binRepository.findById(id)
                .map(existing -> {
                    updatedBin.setId(id);
                    return binRepository.save(updatedBin);
                });
    }

    @Override
    public boolean deleteBin(String id) {
        return binRepository.findById(id)
                .map(bin -> {
                    binRepository.delete(bin);
                    return true;
                })
                .orElse(false);
    }
}
