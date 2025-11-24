package com.wastemanagement.backend.mapper.collection;

import com.wastemanagement.backend.dto.collection.BinReadingRequestDTO;
import com.wastemanagement.backend.dto.collection.BinReadingResponseDTO;
import com.wastemanagement.backend.model.collection.BinReading;

public class BinReadingMapper {

    public static BinReading toEntity(BinReadingRequestDTO dto) {
        if (dto == null) return null;
        BinReading entity = new BinReading();
        entity.setBinId(dto.getBinId());
        entity.setTs(dto.getTs());
        entity.setFillPct(dto.getFillPct());
        entity.setBatteryPct(dto.getBatteryPct());
        entity.setTemperatureC(dto.getTemperatureC());
        entity.setSignalDbm(dto.getSignalDbm());
        return entity;
    }

    public static void merge(BinReading existing, BinReadingRequestDTO dto) {
        if (existing == null || dto == null) return;

        // Update primitives always
        existing.setFillPct(dto.getFillPct());
        existing.setBatteryPct(dto.getBatteryPct());
        existing.setTemperatureC(dto.getTemperatureC());
        existing.setSignalDbm(dto.getSignalDbm());

        // Update references if non-null
        if (dto.getBinId() != null) existing.setBinId(dto.getBinId());
        if (dto.getTs() != null) existing.setTs(dto.getTs());
    }

    public static BinReadingResponseDTO toResponseDTO(BinReading entity) {
        if (entity == null) return null;
        BinReadingResponseDTO dto = new BinReadingResponseDTO();
        dto.setId(entity.getId());
        dto.setBinId(entity.getBinId());
        dto.setTs(entity.getTs());
        dto.setFillPct(entity.getFillPct());
        dto.setBatteryPct(entity.getBatteryPct());
        dto.setTemperatureC(entity.getTemperatureC());
        dto.setSignalDbm(entity.getSignalDbm());
        return dto;
    }
}
