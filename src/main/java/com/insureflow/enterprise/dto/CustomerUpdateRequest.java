package com.insureflow.enterprise.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CustomerUpdateRequest {

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Valid
    private AddressDto address;

    @Valid
    private List<NomineeDto> nominees;
}
