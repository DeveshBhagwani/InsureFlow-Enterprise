package com.insureflow.enterprise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerGrowthResponse {
    private String date; // LocalDate formatted as yyyy-MM-dd
    private long signupsCount;
}
