package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrLeaveRequestDTO;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.RequestType;
import com.group5.ems.entity.User;
import com.group5.ems.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HrLeaveServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @InjectMocks
    private HrLeaveService hrLeaveService;

    private Request leaveReq1;
    private Request leaveReq2;

    @BeforeEach
    void setUp() {
        User user1 = new User();
        user1.setFullName("Alice Smith");

        Department dept1 = new Department();
        dept1.setName("Sales");

        Employee employee1 = new Employee();
        employee1.setUser(user1);
        employee1.setDepartment(dept1);

        RequestType type1 = new RequestType();
        type1.setName("Annual Leave");
        type1.setCategory("ATTENDANCE");

        leaveReq1 = new Request();
        leaveReq1.setId(10L);
        leaveReq1.setEmployee(employee1);
        leaveReq1.setRequestType(type1);
        leaveReq1.setLeaveFrom(LocalDate.now());
        leaveReq1.setLeaveTo(LocalDate.now().plusDays(2));
        leaveReq1.setStatus("PENDING");
        leaveReq1.setContent("Vacation");
        
        leaveReq2 = new Request();
        leaveReq2.setId(11L);
        leaveReq2.setEmployee(employee1);
        leaveReq2.setRequestType(type1);
        leaveReq2.setLeaveFrom(LocalDate.now().minusDays(5));
        leaveReq2.setLeaveTo(LocalDate.now().minusDays(3));
        leaveReq2.setStatus("APPROVED");
        leaveReq2.setContent("Past Vacation");
    }

    @Test
    void testGetPendingLeaveRequests() {
        when(requestRepository.findByStatus("PENDING")).thenReturn(Arrays.asList(leaveReq1));

        List<HrLeaveRequestDTO> result = hrLeaveService.getPendingLeaves();

        assertEquals(1, result.size());
        assertEquals("Alice Smith", result.get(0).employeeName());
        assertEquals("Sales", result.get(0).department());
        assertEquals("Annual Leave", result.get(0).leaveType());
        assertEquals("PENDING", result.get(0).status());
    }

    @Test
    void testGetLeaveHistory() {
        when(requestRepository.findAll()).thenReturn(Arrays.asList(leaveReq1, leaveReq2));

        List<HrLeaveRequestDTO> result = hrLeaveService.getLeaveHistory();

        assertEquals(1, result.size()); // Should only return non-PENDING attendance requests
        assertEquals("APPROVED", result.get(0).status());
    }
}
