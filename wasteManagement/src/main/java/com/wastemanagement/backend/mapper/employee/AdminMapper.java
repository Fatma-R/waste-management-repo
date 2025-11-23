package com.wastemanagement.backend.mapper.employee;

import com.wastemanagement.backend.dto.user.AdminRequestDTO;
import com.wastemanagement.backend.dto.user.AdminResponseDTO;
import com.wastemanagement.backend.model.user.Admin;
import com.wastemanagement.backend.model.user.User;

public class AdminMapper {

    public static Admin toEntity(AdminRequestDTO dto) {
        if (dto == null) return null;

        Admin admin = new Admin();

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        admin.setUser(user);

        return admin;
    }

    public static AdminResponseDTO toResponse(Admin admin) {
        if (admin == null) return null;

        AdminResponseDTO dto = new AdminResponseDTO();
        dto.setId(admin.getId());

        if (admin.getUser() != null) {
            dto.setFullName(admin.getUser().getFullName());
            dto.setEmail(admin.getUser().getEmail());
        }

        return dto;
    }

    public static void updateEntity(Admin admin, AdminRequestDTO dto) {
        if (admin == null || dto == null) return;

        if (admin.getUser() == null) {
            admin.setUser(new User());
        }

        if (dto.getFullName() != null) {
            admin.getUser().setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null) {
            admin.getUser().setEmail(dto.getEmail());
        }
    }
}
