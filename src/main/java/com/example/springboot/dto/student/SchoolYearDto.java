package com.example.springboot.dto.student;

public record SchoolYearDto(
    Integer rowIndex,
    String syStart,
    String semStart,
    String syEnd,
    String semEnd,
    String remarks
) {}
