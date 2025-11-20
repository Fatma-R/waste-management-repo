package com.wastemanagement.backend.controller.user;

import com.wastemanagement.backend.dto.user.AdminRequestDTO;
import com.wastemanagement.backend.dto.user.AdminResponseDTO;
import com.wastemanagement.backend.mapper.employee.AdminMapper;
import com.wastemanagement.backend.model.user.Admin;
import com.wastemanagement.backend.service.user.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping
    public AdminResponseDTO create(@RequestBody AdminRequestDTO dto) {
        Admin admin = adminService.createAdmin(dto);
        return AdminMapper.toResponse(admin);
    }

    @GetMapping("/{id}")
    public AdminResponseDTO getById(@PathVariable String id) {
        Admin admin = adminService.getAdminById(id);
        return AdminMapper.toResponse(admin);
    }

    @GetMapping
    public List<AdminResponseDTO> getAll() {
        return adminService.getAllAdmins()
                .stream()
                .map(AdminMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public AdminResponseDTO update(@PathVariable String id,
                                   @RequestBody AdminRequestDTO dto) {
        Admin admin = adminService.updateAdmin(id, dto);
        return AdminMapper.toResponse(admin);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        adminService.deleteAdmin(id);
    }
}
