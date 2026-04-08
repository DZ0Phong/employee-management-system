package com.group5.ems.service.guest;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group5.ems.entity.JobPost;
import com.group5.ems.repository.JobPostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobPostService {

    private final JobPostRepository jobPostRepository;

    public List<JobPost> getOpenJobs() {
        return jobPostRepository.findByStatus("OPEN");
    }

    public List<JobPost> getJobsByDepartment(Long id) {
        return jobPostRepository.findByDepartmentId(id);
    }

    public JobPost getJobDetail(Long id) {
        return jobPostRepository.findById(id).orElse(null);
    }

    public long countJobsByDepartment(Long deptId) {
        return jobPostRepository.countOpenByDepartment(deptId);
    }

    public String getJobTitle(Long jobId) {
        return jobPostRepository
                .findTitleById(jobId)
                .orElse("the position");
    }
}