package com.example.springboot.dto.student;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParentDto(
    String familyName,
    String firstName,
    String middleName,
    LocalDate birthdate,
    String occupation,
    BigDecimal estIncome,
    String contactNo,
    String email,
    String address
) {}
