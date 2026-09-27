package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.Size;

public record AssignBatchRequest(
    @Size(max = 20, message = "Batch code must not exceed 20 characters.")
    String batchCode
) {}
