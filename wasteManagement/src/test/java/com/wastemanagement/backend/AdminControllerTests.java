package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.user.AdminController;
import com.wastemanagement.backend.dto.user.AdminRequestDTO;
import com.wastemanagement.backend.dto.user.AdminResponseDTO;
import com.wastemanagement.backend.mapper.employee.AdminMapper;
import com.wastemanagement.backend.model.user.Admin;
import com.wastemanagement.backend.model.user.User;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.service.user.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
@ActiveProfiles("test")
class AdminControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtUtil jwtUtils;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AdminRequestDTO requestDTO;
    private Admin admin;
    private AdminResponseDTO adminResponseDTO;

    @BeforeEach
    void setup() {
        requestDTO = new AdminRequestDTO();
        requestDTO.setFullName("Super Admin");
        requestDTO.setEmail("admin@example.com");

        User user = new User();
        user.setFullName(requestDTO.getFullName());
        user.setEmail(requestDTO.getEmail());

        admin = new Admin();
        admin.setId("1");
        admin.setUser(user);

        adminResponseDTO = new AdminResponseDTO();
        adminResponseDTO.setId("1");
        adminResponseDTO.setFullName("Super Admin");
        adminResponseDTO.setEmail("admin@example.com");
    }

    @Test
    void testMapperToEntityAndResponse() {
        Admin mapped = AdminMapper.toEntity(requestDTO);
        assert mapped.getUser() != null;
        assert mapped.getUser().getFullName().equals("Super Admin");
        assert mapped.getUser().getEmail().equals("admin@example.com");

        var responseDTO = AdminMapper.toResponse(admin);
        assert responseDTO.getId().equals("1");
        assert responseDTO.getFullName().equals("Super Admin");
        assert responseDTO.getEmail().equals("admin@example.com");
    }

    @Test
    void testUpdateAdminController() throws Exception {
        AdminResponseDTO updatedResponse = new AdminResponseDTO();
        updatedResponse.setId("1");
        updatedResponse.setFullName("Updated Admin");
        updatedResponse.setEmail("admin@example.com");

        when(adminService.updateAdmin(eq("1"), any())).thenReturn(updatedResponse);
        requestDTO.setFullName("Updated Admin");

        mockMvc.perform(put("/api/v1/admins/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.fullName").value("Updated Admin"))
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void testGetAdminByIdController() throws Exception {
        when(adminService.getAdminById("1")).thenReturn(adminResponseDTO);

        mockMvc.perform(get("/api/v1/admins/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.fullName").value("Super Admin"))
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void testGetAllAdminsController() throws Exception {
        AdminResponseDTO admin2 = new AdminResponseDTO();
        admin2.setId("2");
        admin2.setFullName("Second Admin");
        admin2.setEmail("admin2@example.com");

        List<AdminResponseDTO> list = Arrays.asList(adminResponseDTO, admin2);

        when(adminService.getAllAdmins()).thenReturn(list);

        mockMvc.perform(get("/api/v1/admins")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].fullName").value("Super Admin"))
                .andExpect(jsonPath("$[0].email").value("admin@example.com"))
                .andExpect(jsonPath("$[1].id").value("2"))
                .andExpect(jsonPath("$[1].fullName").value("Second Admin"))
                .andExpect(jsonPath("$[1].email").value("admin2@example.com"));
    }

    @Test
    void testDeleteAdminController() throws Exception {
        doNothing().when(adminService).deleteAdminAndUserByAdminId("1");

        mockMvc.perform(delete("/api/v1/admins/1"))
                .andExpect(status().isOk());

        verify(adminService).deleteAdminAndUserByAdminId("1");
    }
}
