package com.wastemanagement.backend.mapper.employee;

import com.wastemanagement.backend.dto.user.AdminRequestDTO;
import com.wastemanagement.backend.dto.user.AdminResponseDTO;
import com.wastemanagement.backend.model.user.Admin;

public class AdminMapper {

    public static Admin toEntity(AdminRequestDTO dto) {
        Admin admin = new Admin();
        admin.setFullName(dto.getFullName());
        admin.setEmail(dto.getEmail());
        return admin;
    }

    public static AdminResponseDTO toResponse(Admin admin) {
        AdminResponseDTO dto = new AdminResponseDTO();
        dto.setId(admin.getId());
        dto.setFullName(admin.getFullName());
        dto.setEmail(admin.getEmail());
        return dto;
    }

    public static void updateEntity(Admin admin, AdminRequestDTO dto) {
        if (dto.getFullName() != null) {
            admin.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null) {
            admin.setEmail(dto.getEmail());
        }
    }
}
