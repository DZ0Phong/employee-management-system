package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrApplicantDTO;
import com.group5.ems.dto.response.HrRecruitmentDTO;
import com.group5.ems.entity.JobPost;
import com.group5.ems.repository.JobPostRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HrRecruitmentService {

    private final JobPostRepository jobPostRepository;

    public HrRecruitmentService(JobPostRepository jobPostRepository) {
        this.jobPostRepository = jobPostRepository;
    }

    public List<HrRecruitmentDTO> getActiveJobPosts() {
        return jobPostRepository.findAll().stream()
                .filter(job -> "OPEN".equals(job.getStatus()))
                .map(this::mapToJobPostDTO)
                .collect(Collectors.toList());
    }

    public List<HrApplicantDTO> getRecentApplications() {
        // Return dummy data since JobApplication entity is not yet defined
        List<HrApplicantDTO> applicants = new ArrayList<>();
        applicants.add(HrApplicantDTO.builder()
                .id(1L)
                .applicantName("Sarah Jenkins")
                .email("s.jenkins@email.com")
                .appliedJob("Senior React Developer")
                .appliedDate(LocalDateTime.now().minusDays(2))
                .stage("Interviewing")
                .build());
        return applicants;
    }

    private HrRecruitmentDTO mapToJobPostDTO(JobPost jobPost) {
        String departmentName = "N/A";
        if (jobPost.getDepartment() != null) {
            departmentName = jobPost.getDepartment().getName();
        }

        return HrRecruitmentDTO.builder()
                .id(jobPost.getId())
                .jobTitle(jobPost.getTitle())
                .department(departmentName)
                .location("Remote") // Missing physical location in schema
                .status(jobPost.getStatus())
                .applicantCount(0)
                .build();
    }
}
