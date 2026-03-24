package com.group5.ems.repository;

import com.group5.ems.entity.EmployeeBankDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeBankDetailRepository extends JpaRepository<EmployeeBankDetail, Long> {
    
    List<EmployeeBankDetail> findByEmployeeId(Long employeeId);
    
    long countByEmployeeId(Long employeeId);
    
    Optional<EmployeeBankDetail> findByIdAndEmployeeId(Long id, Long employeeId);

    @Modifying
    @Query("UPDATE EmployeeBankDetail e SET e.isPrimary = false WHERE e.employee.id = :employeeId")
    void resetPrimaryAccounts(Long employeeId);
}
