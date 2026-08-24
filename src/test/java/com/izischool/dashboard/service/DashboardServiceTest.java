package com.izischool.dashboard.service;

import com.izischool.dashboard.dto.DashboardStatsDto;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.school.service.SchoolService;
import com.izischool.student.repository.StudentRepository;
import com.izischool.tenant.service.TenantValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private SchoolService schoolService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PaymentScheduleRepository paymentScheduleRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TenantValidationService tenantValidationService;

    @InjectMocks
    private DashboardService dashboardService;

    private School school;

    @BeforeEach
    void setUp() {
        school = School.builder()
                .id(UUID.randomUUID())
                .name("Académie Internationale")
                .code("ACAD-INT")
                .currency("XOF")
                .status(SchoolStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Calcul des métriques financières et taux de recouvrement du Dashboard")
    void testGetDashboardMetrics() {
        UUID schoolId = school.getId();

        when(schoolService.getSchoolById(schoolId)).thenReturn(school);
        when(studentRepository.countBySchool_IdAndDeletedFalse(schoolId)).thenReturn(250L);
        when(paymentScheduleRepository.sumTotalExpectedAmount(schoolId)).thenReturn(new BigDecimal("100000000.00"));
        when(paymentScheduleRepository.sumTotalCollectedAmount(schoolId)).thenReturn(new BigDecimal("75000000.00"));
        when(paymentScheduleRepository.sumTotalOutstandingAmount(schoolId)).thenReturn(new BigDecimal("25000000.00"));
        when(paymentScheduleRepository.sumTotalOverdueAmount(schoolId)).thenReturn(new BigDecimal("5000000.00"));
        when(paymentScheduleRepository.countBySchool_IdAndStatusAndDeletedFalse(schoolId, PaymentScheduleStatus.OVERDUE)).thenReturn(12L);
        when(paymentRepository.countSuccessfulPaymentsBetween(eq(schoolId), any(Instant.class), any(Instant.class))).thenReturn(5L);
        when(paymentRepository.sumSuccessfulPaymentsBetween(eq(schoolId), any(Instant.class), any(Instant.class))).thenReturn(new BigDecimal("1500000.00"));

        DashboardStatsDto stats = dashboardService.getDashboardMetrics(schoolId);

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalStudents()).isEqualTo(250L);
        assertThat(stats.getTotalExpectedAmount()).isEqualByComparingTo("100000000.00");
        assertThat(stats.getTotalCollectedAmount()).isEqualByComparingTo("75000000.00");
        assertThat(stats.getTotalOutstandingAmount()).isEqualByComparingTo("25000000.00");
        assertThat(stats.getTotalOverdueAmount()).isEqualByComparingTo("5000000.00");
        assertThat(stats.getOverdueSchedulesCount()).isEqualTo(12L);
        assertThat(stats.getCollectionRate()).isEqualByComparingTo("75.00");
        assertThat(stats.getCurrency()).isEqualTo("XOF");
    }
}
