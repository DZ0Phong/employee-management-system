package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrPerformanceDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class HrPerformanceServiceTest {

    @InjectMocks
    private HrPerformanceService hrPerformanceService;

    @Test
    void testGetAllAppraisals() {
        // Just tests the dummy data generation for now since Appraisal isn't in SQL schema
        List<HrPerformanceDTO> result = hrPerformanceService.getAllAppraisals();

        assertEquals(2, result.size());
        
        HrPerformanceDTO dto1 = result.get(0);
        assertEquals("Jane Doe", dto1.employeeName());
        assertEquals("Completed", dto1.status());
        assertEquals("A", dto1.score());

        HrPerformanceDTO dto2 = result.get(1);
        assertEquals("David Miller", dto2.employeeName());
        assertEquals("Pending Reviewer", dto2.status());
        assertEquals("-", dto2.score());
    }
}
