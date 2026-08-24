package com.izischool.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorDashboardResponse {

    private UUID academicYearId;
    private String academicYearName;
    private String currency;

    private GlobalEnrollmentStatsResponse enrollment;
    private DirectorPaymentSummaryResponse payments;
    private FeeTypeSummaryResponse registrations;
    private FeeTypeSummaryResponse tuition;
    private MonthlyScheduleStatusResponse currentMonthSchedules;

    private List<ClassEnrollmentStatsResponse> classes;
    private List<FeeCategoryBreakdownResponse> feeCategories;
    private List<PaymentMethodBreakdownResponse> paymentMethods;
    private List<PaymentEvolutionResponse> recentEvolution;
    private List<OverdueStudentPaymentResponse> recentOverdue;
    private List<UpcomingDueDateResponse> upcomingDueDates;
}
