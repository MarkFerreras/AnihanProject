package com.example.springboot.dto.student;

import java.math.BigDecimal;

public record OjtDto(
    String companyName,
    String companyAddress,
    BigDecimal hoursRendered
) {}
