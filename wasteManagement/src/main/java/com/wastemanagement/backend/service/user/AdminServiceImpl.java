package com.wastemanagement.backend.service.user;

import com.wastemanagement.backend.dto.user.AdminRequestDTO;
import com.wastemanagement.backend.mapper.employee.AdminMapper;
import com.wastemanagement.backend.model.user.Admin;
import com.wastemanagement.backend.repository.user.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepo;

    @Override
    public Admin createAdmin(AdminRequestDTO dto) {
        Admin admin = new Admin(
                dto.getFullName(),
                dto.getEmail());
        return adminRepo.save(admin);
    }

    @Override
    public Admin getAdminById(String id) {
        return adminRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    @Override
    public List<Admin> getAllAdmins() {
        return (List<Admin>) adminRepo.findAll();
    }

    @Override
    public Admin updateAdmin(String id, AdminRequestDTO dto) {
        Admin admin = getAdminById(id);
        AdminMapper.updateEntity(admin, dto);
        return adminRepo.save(admin);
    }

    @Override
    public void deleteAdmin(String id) {
        adminRepo.deleteById(id);
    }
}
