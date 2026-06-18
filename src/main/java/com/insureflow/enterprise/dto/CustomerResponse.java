package com.insureflow.enterprise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
    private String kycDocumentPath;
    private String kycStatus;
    private AddressDto address;
    private List<NomineeDto> nominees;
}
