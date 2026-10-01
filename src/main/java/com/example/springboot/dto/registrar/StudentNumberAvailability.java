package com.example.springboot.dto.registrar;

/** Whether a student number is free for a record; {@code assignedTo} is "Last, First" of the other holder, else null. */
public record StudentNumberAvailability(boolean available, String assignedTo) {
}
