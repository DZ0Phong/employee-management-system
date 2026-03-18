package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrPerformanceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrPerformanceService {

    public List<HrPerformanceDTO> getAllAppraisals() {
        // Return mostly dummy data as Appraisal is not in SQL entity schema!
        List<HrPerformanceDTO> list = new ArrayList<>();
        list.add(HrPerformanceDTO.builder()
                .id(1L)
                .employeeName("Jane Doe")
                .department("Engineering")
                .reviewerName("Albert Hudson (Manager)")
                .status("Completed")
                .score("A")
                .build());
                
        list.add(HrPerformanceDTO.builder()
                .id(2L)
                .employeeName("David Miller")
                .department("Sales")
                .reviewerName("Lucy Morgan (Manager)")
                .status("Pending Reviewer")
                .score("-")
                .build());
        return list;
    }
}
