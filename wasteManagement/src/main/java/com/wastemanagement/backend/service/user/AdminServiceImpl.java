package com.wastemanagement.backend.service.user;

import com.wastemanagement.backend.dto.user.AdminRequestDTO;
import com.wastemanagement.backend.dto.user.AdminResponseDTO;
import com.wastemanagement.backend.mapper.employee.AdminMapper;
import com.wastemanagement.backend.model.user.Admin;
import com.wastemanagement.backend.model.user.User;
import com.wastemanagement.backend.repository.UserRepository;
import com.wastemanagement.backend.repository.user.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepo;
    private final UserRepository userRepo;

    @Override
    public AdminResponseDTO createAdmin(AdminRequestDTO dto) {
        Admin admin = AdminMapper.toEntity(dto);
        Admin saved = adminRepo.save(admin);
        return AdminMapper.toResponse(saved);
    }

    @Override
    public AdminResponseDTO createFromUser(User user) {
        Admin admin = new Admin();
        admin.setUser(user);
        Admin saved = adminRepo.save(admin);
        return AdminMapper.toResponse(saved);
    }

    private Admin findAdminOrThrow(String id) {
        return adminRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    @Override
    public AdminResponseDTO getAdminById(String id) {
        Admin admin = findAdminOrThrow(id);
        return AdminMapper.toResponse(admin);
    }

    @Override
    public List<AdminResponseDTO> getAllAdmins() {
        return ((List<Admin>) adminRepo.findAll())
                .stream()
                .map(AdminMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AdminResponseDTO updateAdmin(String id, AdminRequestDTO dto) {
        Admin admin = findAdminOrThrow(id);
        AdminMapper.updateEntity(admin, dto);
        Admin saved = adminRepo.save(admin);
        return AdminMapper.toResponse(saved);
    }

    @Override
    public void deleteAdmin(String id) {
        adminRepo.deleteById(id);
    }

    @Override
    public void deleteAdminAndUserByAdminId(String adminId) {
        Admin admin = findAdminOrThrow(adminId);
        User user = admin.getUser();

        adminRepo.delete(admin);

        if (user != null && user.getId() != null) {
            userRepo.deleteById(user.getId());
        }
    }
}
