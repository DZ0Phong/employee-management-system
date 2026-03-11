package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrApplicantDTO;
import com.group5.ems.dto.response.HrRecruitmentDTO;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.JobPost;
import com.group5.ems.repository.JobPostRepository;
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
public class HrRecruitmentServiceTest {

    @Mock
    private JobPostRepository jobPostRepository;

    @InjectMocks
    private HrRecruitmentService hrRecruitmentService;

    private JobPost jobPost1;
    private JobPost jobPost2;

    @BeforeEach
    void setUp() {
        Department dept1 = new Department();
        dept1.setName("Marketing");

        jobPost1 = new JobPost();
        jobPost1.setId(1L);
        jobPost1.setTitle("SEO Specialist");
        jobPost1.setDepartment(dept1);
        jobPost1.setStatus("OPEN");

        jobPost2 = new JobPost();
        jobPost2.setId(2L);
        jobPost2.setTitle("Marketing Manager");
        jobPost2.setDepartment(dept1);
        jobPost2.setStatus("CLOSED");
    }

    @Test
    void testGetActiveJobPosts() {
        when(jobPostRepository.findAll()).thenReturn(Arrays.asList(jobPost1, jobPost2));

        List<HrRecruitmentDTO> result = hrRecruitmentService.getActiveJobPosts();

        // Should only return OPEN jobs
        assertEquals(1, result.size());
        assertEquals("SEO Specialist", result.get(0).jobTitle());
        assertEquals("Marketing", result.get(0).department());
        assertEquals("OPEN", result.get(0).status());
    }

    @Test
    void testGetRecentApplications() {
        // Just tests the dummy data generation for now since JobApplication isn't implemented
        List<HrApplicantDTO> result = hrRecruitmentService.getRecentApplications();

        assertEquals(1, result.size());
        assertEquals("Sarah Jenkins", result.get(0).applicantName());
        assertEquals("Interviewing", result.get(0).stage());
    }
}
