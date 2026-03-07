package com.group5.ems.repository;

import com.group5.ems.entity.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    List<JobPost> findByStatus(String status);

    List<JobPost> findByDepartmentId(Long departmentId);

    List<JobPost> findByCreatedBy(Long createdBy);
}

