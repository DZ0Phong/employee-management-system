package com.group5.ems.repository;

import com.group5.ems.entity.RequestApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestApprovalHistoryRepository extends JpaRepository<RequestApprovalHistory, Long> {

    List<RequestApprovalHistory> findByRequestIdOrderByActionAtDesc(Long requestId);

    List<RequestApprovalHistory> findByApproverId(Long approverId);
}
