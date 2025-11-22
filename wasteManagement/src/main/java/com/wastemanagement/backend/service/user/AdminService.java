package com.wastemanagement.backend.service.user;

import com.wastemanagement.backend.dto.user.AdminRequestDTO;
import com.wastemanagement.backend.model.user.Admin;

import java.util.List;

public interface AdminService {

    Admin createAdmin(AdminRequestDTO dto);

    Admin getAdminById(String id);

    List<Admin> getAllAdmins();

    Admin updateAdmin(String id, AdminRequestDTO dto);

    void deleteAdmin(String id);
}
