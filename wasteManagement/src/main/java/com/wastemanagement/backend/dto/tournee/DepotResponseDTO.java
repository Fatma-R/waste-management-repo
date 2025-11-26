package com.wastemanagement.backend.dto.tournee;

import com.wastemanagement.backend.dto.GeoJSONPointDTO;
import lombok.Data;

@Data
public class DepotResponseDTO {
    private String name;
    private String address;
    private GeoJSONPointDTO location;
}
