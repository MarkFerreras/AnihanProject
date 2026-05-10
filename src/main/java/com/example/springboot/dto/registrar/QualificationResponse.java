package com.example.springboot.dto.registrar;

import com.example.springboot.model.Qualification;

public record QualificationResponse(
        Integer qualificationCode,
        String qualificationName,
        String qualificationDescription) {
    public static QualificationResponse from(Qualification q) {
        return new QualificationResponse(
                q.getQualificationCode(),
                q.getQualificationName(),
                q.getQualificationDescription());
    }
}
