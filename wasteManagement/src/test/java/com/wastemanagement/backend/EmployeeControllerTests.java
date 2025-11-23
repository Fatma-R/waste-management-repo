package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.user.EmployeeController;
import com.wastemanagement.backend.dto.user.EmployeeRequestDTO;
import com.wastemanagement.backend.dto.user.EmployeeResponseDTO;
import com.wastemanagement.backend.mapper.employee.EmployeeMapper;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.user.Skill;
import com.wastemanagement.backend.model.user.User;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.service.user.EmployeeService;
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
        controllers = EmployeeController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
@ActiveProfiles("test")
class EmployeeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private JwtUtil jwtUtils;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EmployeeRequestDTO requestDTO;
    private Employee employee;
    private EmployeeResponseDTO employeeResponseDTO;

    @BeforeEach
    void setup() {
        requestDTO = new EmployeeRequestDTO();
        requestDTO.setFullName("John Doe");
        requestDTO.setEmail("john.doe@example.com");
        requestDTO.setSkill(Skill.DRIVER);

        User user = new User();
        user.setFullName(requestDTO.getFullName());
        user.setEmail(requestDTO.getEmail());

        employee = new Employee();
        employee.setId("1");
        employee.setUser(user);
        employee.setSkill(Skill.DRIVER);

        employeeResponseDTO = new EmployeeResponseDTO();
        employeeResponseDTO.setId("1");
        employeeResponseDTO.setFullName("John Doe");
        employeeResponseDTO.setEmail("john.doe@example.com");
        employeeResponseDTO.setSkill(Skill.DRIVER);
    }

    @Test
    void testMapperToEntityAndResponse() {
        Employee mapped = EmployeeMapper.toEntity(requestDTO);
        assert mapped.getUser() != null;
        assert mapped.getUser().getFullName().equals("John Doe");
        assert mapped.getUser().getEmail().equals("john.doe@example.com");
        assert mapped.getSkill() == Skill.DRIVER;

        var responseDTO = EmployeeMapper.toResponse(employee);
        assert responseDTO.getId().equals("1");
        assert responseDTO.getFullName().equals("John Doe");
        assert responseDTO.getEmail().equals("john.doe@example.com");
        assert responseDTO.getSkill() == Skill.DRIVER;
    }

    @Test
    void testUpdateEmployeeController() throws Exception {
        EmployeeResponseDTO updated = new EmployeeResponseDTO();
        updated.setId("1");
        updated.setFullName("Updated Name");
        updated.setEmail("john.doe@example.com");
        updated.setSkill(Skill.AGENT);

        when(employeeService.updateEmployee(eq("1"), any())).thenReturn(updated);

        requestDTO.setFullName("Updated Name");
        requestDTO.setSkill(Skill.AGENT);

        mockMvc.perform(put("/api/v1/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.skill").value("AGENT"));
    }

    @Test
    void testGetEmployeeByIdController() throws Exception {
        when(employeeService.getEmployeeById("1")).thenReturn(employeeResponseDTO);

        mockMvc.perform(get("/api/v1/employees/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.skill").value("DRIVER"));
    }

    @Test
    void testGetAllEmployeesController() throws Exception {
        EmployeeResponseDTO emp2 = new EmployeeResponseDTO();
        emp2.setId("2");
        emp2.setFullName("Jane Roe");
        emp2.setEmail("jane.roe@example.com");
        emp2.setSkill(Skill.AGENT);

        List<EmployeeResponseDTO> list = Arrays.asList(employeeResponseDTO, emp2);

        when(employeeService.getAllEmployees()).thenReturn(list);

        mockMvc.perform(get("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john.doe@example.com"))
                .andExpect(jsonPath("$[0].skill").value("DRIVER"))
                .andExpect(jsonPath("$[1].id").value("2"))
                .andExpect(jsonPath("$[1].fullName").value("Jane Roe"))
                .andExpect(jsonPath("$[1].email").value("jane.roe@example.com"))
                .andExpect(jsonPath("$[1].skill").value("AGENT"));
    }

    @Test
    void testDeleteEmployeeController() throws Exception {
        doNothing().when(employeeService).deleteEmployeeAndUserByEmployeeId("1");

        mockMvc.perform(delete("/api/v1/employees/1"))
                .andExpect(status().isOk());

        verify(employeeService).deleteEmployeeAndUserByEmployeeId("1");
    }
}
