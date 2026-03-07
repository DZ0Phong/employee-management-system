package com.group5.ems.repository;

import com.group5.ems.entity.AccountRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {

    List<AccountRequest> findByEmployeeId(Long employeeId);

    List<AccountRequest> findByStatus(String status);

    List<AccountRequest> findByRequestedBy(Long requestedBy);
}
