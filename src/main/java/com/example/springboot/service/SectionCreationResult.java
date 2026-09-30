package com.example.springboot.service;

import com.example.springboot.dto.registrar.SectionResponse;

/**
 * What {@link ClassManagementService#createSection} did, so the controller can write one
 * audit row per auto-created batch/course in addition to the section row.
 */
public record SectionCreationResult(SectionResponse section, boolean courseCreated, boolean batchCreated) {
}
