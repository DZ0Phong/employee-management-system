package com.group5.hrm.repository;

import com.group5.hrm.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssignedTo(Long assignedTo);

    List<Task> findByAssignedBy(Long assignedBy);

    List<Task> findByAssignedToAndStatus(Long assignedTo, String status);
}

