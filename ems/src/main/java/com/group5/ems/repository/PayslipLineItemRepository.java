package com.group5.ems.repository;

import com.group5.ems.entity.PayslipLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PayslipLineItemRepository extends JpaRepository<PayslipLineItem, Long> {
    List<PayslipLineItem> findByPayslipId(Long payslipId);
}
