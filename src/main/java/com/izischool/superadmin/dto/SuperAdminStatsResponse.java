package com.izischool.superadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminStatsResponse {

    private long totalSchools;
    private long activeSchools;
    private long suspendedSchools;
    private long inactiveSchools;
    private long totalStudents;
    private BigDecimal totalCollectedRevenue;
    private long totalSuccessfulPayments;
    private long totalUsers;
}
