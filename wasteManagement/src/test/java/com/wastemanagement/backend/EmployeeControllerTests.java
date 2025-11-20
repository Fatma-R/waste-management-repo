package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.controller.employee.EmployeeController;
import com.wastemanagement.backend.dto.employee.EmployeeRequestDTO;
import com.wastemanagement.backend.mapper.EmployeeMapper;
import com.wastemanagement.backend.model.employee.Employee;
import com.wastemanagement.backend.model.employee.Skill;
import com.wastemanagement.backend.security.JwtUtil;
import com.wastemanagement.backend.service.CustomUserDetailsService;
import com.wastemanagement.backend.service.employee.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = EmployeeController.class,
        // Completely exclude ALL Spring Security auto-configuration
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
class EmployeeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    // 👉 Needed so AuthTokenFilter can be created without failing
    @MockBean
    private JwtUtil jwtUtils;

    // 👉 Needed because AuthTokenFilter autowires this too
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EmployeeRequestDTO requestDTO;
    private Employee employee;

    @BeforeEach
    void setup() {
        requestDTO = new EmployeeRequestDTO();
        requestDTO.setFullName("John Doe");
        requestDTO.setEmail("john@example.com");
        requestDTO.setSkill("DRIVER");

        employee = new Employee();
        employee.setId("1");
        employee.setFullName("John Doe");
        employee.setEmail("john@example.com");
        employee.setSkill(Skill.DRIVER);
    }

    @Test
    void testServiceCreateEmployee() {
        when(employeeService.createEmployee(requestDTO)).thenReturn(employee);

        Employee result = employeeService.createEmployee(requestDTO);

        assert result != null;
        assert result.getFullName().equals("John Doe");
        assert result.getSkill() == Skill.DRIVER;

        verify(employeeService).createEmployee(requestDTO);
    }

    @Test
    void testMapperToEntityAndResponse() {
        Employee mapped = EmployeeMapper.toEntity(requestDTO);
        assert mapped.getFullName().equals("John Doe");
        assert mapped.getSkill() == Skill.DRIVER;

        var responseDTO = EmployeeMapper.toResponse(employee);
        assert responseDTO.getId().equals("1");
        assert responseDTO.getFullName().equals("John Doe");
        assert responseDTO.getSkill().equals("DRIVER");
    }

    @Test
    void testCreateEmployeeController() throws Exception {
        when(employeeService.createEmployee(any())).thenReturn(employee);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.skill").value("DRIVER"));
    }

    @Test
    void testUpdateEmployeeController() throws Exception {
        Employee updated = new Employee();
        updated.setId("1");
        updated.setFullName("John Updated");
        updated.setEmail("john@example.com");
        updated.setSkill(Skill.DRIVER);

        when(employeeService.updateEmployee(eq("1"), any())).thenReturn(updated);
        requestDTO.setFullName("John Updated");

        mockMvc.perform(put("/api/v1/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Updated"));
    }

    @Test
    void testGetEmployeeByIdController() throws Exception {
        when(employeeService.getEmployeeById("1")).thenReturn(employee);

        mockMvc.perform(get("/api/v1/employees/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.skill").value("DRIVER"));
    }

    @Test
    void testGetAllEmployeesController() throws Exception {
        Employee employee2 = new Employee();
        employee2.setId("2");
        employee2.setFullName("Jane Smith");
        employee2.setEmail("jane@example.com");
        employee2.setSkill(Skill.DRIVER);

        when(employeeService.getAllEmployees()).thenReturn(Arrays.asList(employee, employee2));

        mockMvc.perform(get("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"))
                .andExpect(jsonPath("$[1].id").value("2"))
                .andExpect(jsonPath("$[1].fullName").value("Jane Smith"));
    }

    @Test
    void testDeleteEmployeeController() throws Exception {
        doNothing().when(employeeService).deleteEmployee("1");

        mockMvc.perform(delete("/api/v1/employees/1"))
                .andExpect(status().isOk());

        verify(employeeService).deleteEmployee("1");
    }

    // Security-related tests intentionally left out here.
    // If later you want to test AuthTokenFilter + JwtUtil + @PreAuthorize,
    // we’ll create a separate @SpringBootTest with full security enabled.
}
