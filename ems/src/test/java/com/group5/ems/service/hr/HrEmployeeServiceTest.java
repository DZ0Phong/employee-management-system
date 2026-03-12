package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrEmployeeDTO;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Position;
import com.group5.ems.entity.User;
import com.group5.ems.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HrEmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private HrEmployeeService hrEmployeeService;

    private Employee employee1;

    @BeforeEach
    void setUp() {
        User user1 = new User();
        user1.setFullName("John Doe");
        user1.setEmail("john.doe@company.local");
        user1.setPhone("012-345-6789");

        Department dept1 = new Department();
        dept1.setName("Engineering");

        Position pos1 = new Position();
        pos1.setName("Developer");

        employee1 = new Employee();
        employee1.setId(1L);
        employee1.setEmployeeCode("EMP-001");
        employee1.setStatus("Active");
        employee1.setUser(user1);
        employee1.setDepartment(dept1);
        employee1.setPosition(pos1);
    }

    @Test
    void testGetAllEmployees() {
        // Arrange
        when(employeeRepository.findAll()).thenReturn(Arrays.asList(employee1));

        // Act
        List<HrEmployeeDTO> result = hrEmployeeService.getAllEmployees();

        // Assert
        assertEquals(1, result.size());
        
        HrEmployeeDTO dto = result.get(0);
        assertEquals("EMP-001", dto.code());
        assertEquals("Active", dto.status());
        assertEquals("Engineering", dto.department());
        assertEquals("Developer", dto.position());
        assertEquals("John Doe", dto.fullName());
        assertEquals("JD", dto.initials());
        assertEquals("john.doe@company.local", dto.email());
        assertEquals("012-345-6789", dto.phone());
    }
}

