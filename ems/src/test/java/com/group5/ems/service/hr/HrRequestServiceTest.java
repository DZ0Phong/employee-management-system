package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrRequestDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HrRequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @InjectMocks
    private HrRequestService hrRequestService;

    private Request workflowReq1;

    @BeforeEach
    void setUp() {
         User user1 = new User();
         user1.setFullName("Eve Adams");

         Department dept1 = new Department();
         dept1.setName("Finance");

         Employee employee1 = new Employee();
         employee1.setUser(user1);
         employee1.setDepartment(dept1);

         RequestType type1 = new RequestType();
         type1.setName("Salary Advance");
         type1.setCategory("HR_STATUS");

         workflowReq1 = new Request();
         workflowReq1.setId(50L);
         workflowReq1.setEmployee(employee1);
         workflowReq1.setRequestType(type1);
         workflowReq1.setTitle("Requesting advance");
         workflowReq1.setContent("Medical emergency");
         workflowReq1.setStatus("PENDING");
         workflowReq1.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void testGetAllWorkflowRequests() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Request> page = new PageImpl<>(Arrays.asList(workflowReq1), pageable, 1);
        when(requestRepository.findWorkflowRequests(any(Pageable.class))).thenReturn(page);

        Page<HrRequestDTO> result = hrRequestService.getAllWorkflowRequests(pageable);

        assertEquals(1, result.getContent().size());
        
        HrRequestDTO dto = result.getContent().get(0);
        assertEquals("Eve Adams", dto.requestedBy());
        assertEquals("Finance", dto.department());
        assertEquals("Requesting advance", dto.title());
        assertEquals("Salary Advance", dto.category());
        assertEquals("PENDING", dto.status());
    }
}
