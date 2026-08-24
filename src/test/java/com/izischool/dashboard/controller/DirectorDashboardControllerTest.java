package com.izischool.dashboard.controller;

import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.response.PageResponse;
import com.izischool.dashboard.dto.ClassEnrollmentStatsResponse;
import com.izischool.dashboard.dto.DirectorDashboardResponse;
import com.izischool.dashboard.dto.DirectorPaymentSummaryResponse;
import com.izischool.dashboard.dto.FeeCategoryBreakdownResponse;
import com.izischool.dashboard.dto.FeeTypeSummaryResponse;
import com.izischool.dashboard.dto.GlobalEnrollmentStatsResponse;
import com.izischool.dashboard.dto.MonthlyScheduleStatusResponse;
import com.izischool.dashboard.dto.OverdueStudentPaymentResponse;
import com.izischool.dashboard.dto.PaymentEvolutionResponse;
import com.izischool.dashboard.dto.PaymentMethodBreakdownResponse;
import com.izischool.dashboard.dto.StudentFinancialStatusResponse;
import com.izischool.dashboard.dto.UpcomingDueDateResponse;
import com.izischool.dashboard.service.DirectorDashboardService;
import com.izischool.finance.domain.FeeType;
import com.izischool.payment.domain.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectorDashboardControllerTest {

    @Mock
    private DirectorDashboardService directorDashboardService;

    @Mock
    private CurrentUserContextService currentUserContextService;

    @InjectMocks
    private DirectorDashboardController directorDashboardController;

    private UUID schoolId;
    private UUID academicYearId;

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
        academicYearId = UUID.randomUUID();
        when(currentUserContextService.getRequiredSchoolId()).thenReturn(schoolId);
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard - Synthèse globale")
    void testGetDirectorDashboard() {
        DirectorDashboardResponse mockResponse = DirectorDashboardResponse.builder()
                .academicYearId(academicYearId)
                .academicYearName("2026-2027")
                .currency("XOF")
                .build();

        when(directorDashboardService.getDirectorDashboard(schoolId, academicYearId)).thenReturn(mockResponse);

        ResponseEntity<DirectorDashboardResponse> response = directorDashboardController.getDirectorDashboard(academicYearId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAcademicYearName()).isEqualTo("2026-2027");
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/enrollment - Effectifs globaux")
    void testGetGlobalEnrollment() {
        GlobalEnrollmentStatsResponse mockResponse = GlobalEnrollmentStatsResponse.builder()
                .totalStudents(450)
                .totalClasses(15)
                .totalCapacity(500)
                .totalEnrolledStudents(450)
                .totalAvailableSeats(50)
                .globalEnrollmentRate(90.0)
                .build();

        when(directorDashboardService.getGlobalEnrollmentStats(schoolId, academicYearId)).thenReturn(mockResponse);

        ResponseEntity<GlobalEnrollmentStatsResponse> response = directorDashboardController.getGlobalEnrollment(academicYearId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalStudents()).isEqualTo(450);
        assertThat(response.getBody().getGlobalEnrollmentRate()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/classes - Situation des classes")
    void testGetClassesStats() {
        ClassEnrollmentStatsResponse classStats = ClassEnrollmentStatsResponse.builder()
                .classId(UUID.randomUUID())
                .className("6ème A")
                .capacity(30)
                .enrolledCount(28)
                .availableSeats(2)
                .occupancyRate(93.33)
                .build();

        when(directorDashboardService.getClassesEnrollmentStats(schoolId, academicYearId)).thenReturn(List.of(classStats));

        ResponseEntity<List<ClassEnrollmentStatsResponse>> response = directorDashboardController.getClassesStats(academicYearId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getClassName()).isEqualTo("6ème A");
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/classes/{classId} - Détail classe")
    void testGetClassStats() {
        UUID classId = UUID.randomUUID();
        ClassEnrollmentStatsResponse classStats = ClassEnrollmentStatsResponse.builder()
                .classId(classId)
                .className("Terminales S")
                .capacity(35)
                .enrolledCount(35)
                .availableSeats(0)
                .occupancyRate(100.0)
                .build();

        when(directorDashboardService.getClassEnrollmentStats(schoolId, classId)).thenReturn(classStats);

        ResponseEntity<ClassEnrollmentStatsResponse> response = directorDashboardController.getClassStats(classId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClassName()).isEqualTo("Terminales S");
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/payments/summary - Synthèse financière")
    void testGetPaymentSummary() {
        DirectorPaymentSummaryResponse summary = DirectorPaymentSummaryResponse.builder()
                .totalExpected(new BigDecimal("100000000.00"))
                .totalCollected(new BigDecimal("85000000.00"))
                .totalRemaining(new BigDecimal("15000000.00"))
                .collectionRate(new BigDecimal("85.00"))
                .currency("XOF")
                .build();

        when(directorDashboardService.getPaymentSummary(schoolId, academicYearId)).thenReturn(summary);

        ResponseEntity<DirectorPaymentSummaryResponse> response = directorDashboardController.getPaymentSummary(academicYearId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCollectionRate()).isEqualByComparingTo("85.00");
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/registrations - Frais d'inscription")
    void testGetRegistrationSummary() {
        FeeTypeSummaryResponse summary = FeeTypeSummaryResponse.builder()
                .feeType(FeeType.REGISTRATION)
                .feeTypeName("Frais d'inscription")
                .totalExpected(new BigDecimal("20000000.00"))
                .totalCollected(new BigDecimal("19500000.00"))
                .collectionRate(new BigDecimal("97.50"))
                .build();

        when(directorDashboardService.getFeeTypeSummary(schoolId, academicYearId, FeeType.REGISTRATION)).thenReturn(summary);

        ResponseEntity<FeeTypeSummaryResponse> response = directorDashboardController.getRegistrationSummary(academicYearId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFeeType()).isEqualTo(FeeType.REGISTRATION);
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/tuition - Frais de scolarité")
    void testGetTuitionSummary() {
        FeeTypeSummaryResponse summary = FeeTypeSummaryResponse.builder()
                .feeType(FeeType.TUITION)
                .feeTypeName("Frais de scolarité / Mensualités")
                .totalExpected(new BigDecimal("80000000.00"))
                .totalCollected(new BigDecimal("65000000.00"))
                .collectionRate(new BigDecimal("81.25"))
                .build();

        when(directorDashboardService.getFeeTypeSummary(schoolId, academicYearId, FeeType.TUITION)).thenReturn(summary);

        ResponseEntity<FeeTypeSummaryResponse> response = directorDashboardController.getTuitionSummary(academicYearId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFeeType()).isEqualTo(FeeType.TUITION);
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/schedules/monthly - Échéances mensuelles")
    void testGetMonthlyScheduleStatus() {
        MonthlyScheduleStatusResponse monthly = MonthlyScheduleStatusResponse.builder()
                .month(10)
                .year(2026)
                .periodLabel("Octobre 2026")
                .totalExpected(new BigDecimal("15000000.00"))
                .totalCollected(new BigDecimal("12000000.00"))
                .collectionRate(new BigDecimal("80.00"))
                .build();

        when(directorDashboardService.getMonthlyScheduleStatus(schoolId, academicYearId, 10, 2026)).thenReturn(monthly);

        ResponseEntity<MonthlyScheduleStatusResponse> response = directorDashboardController.getMonthlyScheduleStatus(
                academicYearId, 10, 2026);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPeriodLabel()).isEqualTo("Octobre 2026");
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/students/financial-status - Statuts financiers élèves")
    void testGetStudentsFinancialStatus() {
        StudentFinancialStatusResponse stdStatus = StudentFinancialStatusResponse.builder()
                .studentId(UUID.randomUUID())
                .studentNumber("STD-001")
                .studentName("Mamadou Diallo")
                .status("UP_TO_DATE")
                .build();

        PageResponse<StudentFinancialStatusResponse> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(stdStatus), PageRequest.of(0, 20), 1));

        when(directorDashboardService.getStudentsFinancialStatus(eq(schoolId), eq(academicYearId), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        ResponseEntity<PageResponse<StudentFinancialStatusResponse>> response = directorDashboardController.getStudentsFinancialStatus(
                academicYearId, null, null, null, 0, 20);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/payments/by-category - Répartition catégories")
    void testGetFeeCategoryBreakdown() {
        FeeCategoryBreakdownResponse breakdown = FeeCategoryBreakdownResponse.builder()
                .feeName("Cantine Scolaire")
                .feeType(FeeType.OTHER)
                .expectedAmount(new BigDecimal("5000000.00"))
                .collectedAmount(new BigDecimal("4500000.00"))
                .percentageOfTotalCollected(new BigDecimal("10.00"))
                .build();

        when(directorDashboardService.getFeeCategoryBreakdown(schoolId, academicYearId)).thenReturn(List.of(breakdown));

        ResponseEntity<List<FeeCategoryBreakdownResponse>> response = directorDashboardController.getFeeCategoryBreakdown(academicYearId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getFeeName()).isEqualTo("Cantine Scolaire");
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/payments/by-method - Répartition méthodes")
    void testGetPaymentMethodBreakdown() {
        PaymentMethodBreakdownResponse methodBreakdown = PaymentMethodBreakdownResponse.builder()
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .paymentMethodLabel("Mobile Money (Wave / Orange Money)")
                .totalAmount(new BigDecimal("50000000.00"))
                .transactionCount(120)
                .percentage(new BigDecimal("70.00"))
                .build();

        when(directorDashboardService.getPaymentMethodBreakdown(schoolId, academicYearId, null, null))
                .thenReturn(List.of(methodBreakdown));

        ResponseEntity<List<PaymentMethodBreakdownResponse>> response = directorDashboardController.getPaymentMethodBreakdown(
                academicYearId, null, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getPaymentMethod()).isEqualTo(PaymentMethod.MOBILE_MONEY);
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/payments/evolution - Évolution chronologique")
    void testGetPaymentEvolution() {
        PaymentEvolutionResponse evo = PaymentEvolutionResponse.builder()
                .period("2026-08")
                .collectedAmount(new BigDecimal("12000000.00"))
                .transactionCount(45)
                .build();

        when(directorDashboardService.getPaymentEvolution(schoolId, academicYearId, "MONTH", null, null))
                .thenReturn(List.of(evo));

        ResponseEntity<List<PaymentEvolutionResponse>> response = directorDashboardController.getPaymentEvolution(
                academicYearId, "MONTH", null, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getPeriod()).isEqualTo("2026-08");
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/overdue - Impayés & Retards")
    void testGetOverdueStudents() {
        OverdueStudentPaymentResponse overdue = OverdueStudentPaymentResponse.builder()
                .studentId(UUID.randomUUID())
                .studentName("Awa Diop")
                .overdueAmount(new BigDecimal("30000.00"))
                .daysOverdue(15)
                .arrearsBucket("8-30j")
                .parentName("Oumar Diop")
                .parentPhone("+221770001122")
                .build();

        PageResponse<OverdueStudentPaymentResponse> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(overdue), PageRequest.of(0, 20), 1));

        when(directorDashboardService.getOverdueStudents(eq(schoolId), eq(academicYearId), any(), any()))
                .thenReturn(pageResponse);

        ResponseEntity<PageResponse<OverdueStudentPaymentResponse>> response = directorDashboardController.getOverdueStudents(
                academicYearId, null, 0, 20);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent().get(0).getStudentName()).isEqualTo("Awa Diop");
    }

    @Test
    @DisplayName("GET /api/v1/director/dashboard/upcoming-due-dates - Échéances à venir")
    void testGetUpcomingDueDates() {
        UpcomingDueDateResponse upcoming = UpcomingDueDateResponse.builder()
                .dueDate(LocalDate.now().plusDays(5))
                .feeName("Mensualité Novembre")
                .totalAmountExpected(new BigDecimal("10000000.00"))
                .totalStudentsCount(100)
                .isToday(false)
                .daysRemaining(5)
                .build();

        when(directorDashboardService.getUpcomingDueDates(schoolId, academicYearId, 30))
                .thenReturn(List.of(upcoming));

        ResponseEntity<List<UpcomingDueDateResponse>> response = directorDashboardController.getUpcomingDueDates(
                academicYearId, 30);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getDaysRemaining()).isEqualTo(5);
    }
}
