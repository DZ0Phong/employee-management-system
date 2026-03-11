package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiversityDataDTO {
    private List<String> labels; // "Male", "Female", "Non-binary"
    private List<Integer> values; // phần trăm
    private List<String> colors; // màu sắc biểu đồ
}