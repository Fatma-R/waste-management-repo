// java
package com.wastemanagement.backend.controller.user;

import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.dto.user.EmployeeRequestDTO;
import com.wastemanagement.backend.dto.user.EmployeeResponseDTO;
import com.wastemanagement.backend.service.user.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // La création via ce controleur reste désactivée car on force le passage par AuthController.
    // @PostMapping
    // public EmployeeResponseDTO create(@RequestBody EmployeeRequestDTO dto) {
    //     return employeeService.createEmployee(dto);
    // }

    @GetMapping("/{id}")
    public EmployeeResponseDTO getById(@PathVariable String id) {
        return employeeService.getEmployeeById(id);
    }

    @GetMapping
    public List<EmployeeResponseDTO> getAll() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/by-email/{email}")
    public EmployeeResponseDTO getByEmail(@PathVariable String email) {
        return employeeService.getEmployeeByEmail(email);
    }

    @PutMapping("/{id}")
    public EmployeeResponseDTO update(
            @PathVariable String id,
            @RequestBody EmployeeRequestDTO dto) {
        return employeeService.updateEmployee(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        employeeService.deleteEmployeeAndUserByEmployeeId(id);
    }

    @PostMapping("/available-for-tournee")
    public ResponseEntity<List<EmployeeResponseDTO>> getAvailableEmployeesForTournee(
            @RequestBody TourneeResponseDTO tournee) {
        List<EmployeeResponseDTO> availableEmployees = employeeService.getAvailableEmployeeForTournee(tournee);
        return ResponseEntity.ok(availableEmployees);
    }
    
}
