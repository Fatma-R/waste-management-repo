package com.wastemanagement.backend.controller.user;

import com.wastemanagement.backend.dto.user.AdminRequestDTO;
import com.wastemanagement.backend.dto.user.AdminResponseDTO;
import com.wastemanagement.backend.service.user.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // Création désactivée (passage par AuthController).
    // @PostMapping
    // public AdminResponseDTO create(@RequestBody AdminRequestDTO dto) {
    //     return adminService.createAdmin(dto);
    // }

    @GetMapping("/{id}")
    public AdminResponseDTO getById(@PathVariable String id) {
        return adminService.getAdminById(id);
    }

    @GetMapping
    public List<AdminResponseDTO> getAll() {
        return adminService.getAllAdmins();
    }

    @PutMapping("/{id}")
    public AdminResponseDTO update(@PathVariable String id,
                                   @RequestBody AdminRequestDTO dto) {
        return adminService.updateAdmin(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        adminService.deleteAdminAndUserByAdminId(id);
    }
}
