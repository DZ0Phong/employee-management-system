package com.group5.ems.service.employee;

import com.group5.ems.dto.request.CreateLeaveRequestDTO;
import com.group5.ems.dto.response.LeaveBalanceDTO;
import com.group5.ems.dto.response.LeaveRequestDTO;

import java.util.List;

public interface LeaveService {
    List<LeaveBalanceDTO> getLeaveBalances(Long employeeId);
    List<LeaveRequestDTO> getLeaveHistory(Long employeeId);
    void createLeaveRequest(Long employeeId, CreateLeaveRequestDTO dto);
}