package com.izischool.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalEnrollmentStatsResponse {

    private UUID academicYearId;
    private String academicYearName;
    private long totalStudents;
    private long totalClasses;
    private long totalCapacity;
    private long totalEnrolledStudents;
    private long totalAvailableSeats;
    private double globalEnrollmentRate;
    private long pendingRegistrations;
    private long activeRegistrations;
    private long completedRegistrations;
    private long cancelledRegistrations;
}
