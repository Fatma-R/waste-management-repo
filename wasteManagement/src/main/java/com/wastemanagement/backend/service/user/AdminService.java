package com.wastemanagement.backend.service.user;

import com.wastemanagement.backend.dto.user.AdminRequestDTO;
import com.wastemanagement.backend.dto.user.AdminResponseDTO;
import com.wastemanagement.backend.model.user.User;

import java.util.List;

public interface AdminService {

    // plus vraiment utilisé. on force le passage par AuthController
    AdminResponseDTO createAdmin(AdminRequestDTO dto);

    AdminResponseDTO createFromUser(User user);

    AdminResponseDTO getAdminById(String id);

    List<AdminResponseDTO> getAllAdmins();

    AdminResponseDTO updateAdmin(String id, AdminRequestDTO dto);

    // plus vraiment utilisé. on utilise plutot deleteAdminAndUserByAdminId dans AuthController
    void deleteAdmin(String id);

    void deleteAdminAndUserByAdminId(String adminId);
}
