package com.wastemanagement.backend.vroom;

import com.wastemanagement.backend.vroom.dto.VroomRequest;
import com.wastemanagement.backend.vroom.dto.VroomSolution;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class VroomClient {

    private final RestTemplate restTemplate;

    // URL of VROOM server (vroom-docker default)
    @Value("${vroom.url:http://localhost:3000}")
    private String vroomUrl;

    public VroomSolution optimize(VroomRequest request) {
        ResponseEntity<VroomSolution> response =
                restTemplate.postForEntity(vroomUrl, request, VroomSolution.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to call VROOM, status: " + response.getStatusCode());
        }

        VroomSolution solution = response.getBody();
        if (solution.getCode() != 0) {
            throw new IllegalStateException("VROOM error: " + solution.getError());
        }

        return solution;
    }
}
