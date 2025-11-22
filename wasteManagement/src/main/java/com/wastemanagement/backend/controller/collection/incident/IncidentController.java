package com.wastemanagement.backend.controller.collection.incident;

import com.wastemanagement.backend.dto.collection.incident.IncidentRequestDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentResponseDTO;
import com.wastemanagement.backend.service.collection.incident.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public EntityModel<IncidentResponseDTO> create(@Valid @RequestBody IncidentRequestDTO dto) {
        IncidentResponseDTO response = incidentService.createIncident(dto);

        return EntityModel.of(
                response,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(IncidentController.class).get(response.getId())).withSelfRel()
        );
    }

    @GetMapping("/{id}")
    public EntityModel<IncidentResponseDTO> get(@PathVariable String id) {
        IncidentResponseDTO response = incidentService.getIncident(id);

        return EntityModel.of(
                response,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(IncidentController.class).get(id)).withSelfRel()
        );
    }

    @GetMapping
    public List<IncidentResponseDTO> getAll() {
        return incidentService.getAllIncidents();
    }

    @PutMapping("/{id}")
    public EntityModel<IncidentResponseDTO> update(@PathVariable String id,
                                                   @Valid @RequestBody IncidentRequestDTO dto) {
        IncidentResponseDTO response = incidentService.updateIncident(id, dto);

        return EntityModel.of(
                response,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(IncidentController.class).get(id)).withSelfRel()
        );
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        incidentService.deleteIncident(id);
    }
}
