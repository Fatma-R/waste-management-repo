package com.wastemanagement.backend.dto.tournee;

import com.wastemanagement.backend.dto.GeoJSONPointDTO;
import lombok.Data;

@Data
public class DepotRequestDTO {
    private String id;
    private String name;
    private String address;
    private GeoJSONPointDTO location;
}
