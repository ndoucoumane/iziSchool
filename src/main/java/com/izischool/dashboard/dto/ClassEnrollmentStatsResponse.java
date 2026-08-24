package com.izischool.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollmentStatsResponse {

    private UUID classId;
    private String className;
    private String classCode;
    private String gradeLevelName;
    private Integer capacity;
    private long enrolledCount;
    private long availableSeats;
    private double occupancyRate;
    private long pendingCount;
    private long activeCount;
    private BigDecimal totalExpected;
    private BigDecimal totalCollected;
    private BigDecimal totalRemaining;
    private BigDecimal collectionRate;
    private String currency;
}
