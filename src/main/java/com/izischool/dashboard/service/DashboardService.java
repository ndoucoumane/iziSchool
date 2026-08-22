package com.izischool.dashboard.service;

import com.izischool.common.util.MoneyUtils;
import com.izischool.dashboard.dto.DashboardStatsDto;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.school.domain.School;
import com.izischool.school.service.SchoolService;
import com.izischool.student.repository.StudentRepository;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SchoolService schoolService;
    private final StudentRepository studentRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final PaymentRepository paymentRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard_stats", key = "#schoolId", unless = "#result == null")
    public DashboardStatsDto getDashboardMetrics(UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        School school = schoolService.getSchoolById(schoolId);

        long totalStudents = studentRepository.countBySchool_IdAndDeletedFalse(schoolId);

        BigDecimal totalExpected = MoneyUtils.scale(paymentScheduleRepository.sumTotalExpectedAmount(schoolId));
        BigDecimal totalCollected = MoneyUtils.scale(paymentScheduleRepository.sumTotalCollectedAmount(schoolId));
        BigDecimal totalOutstanding = MoneyUtils.scale(paymentScheduleRepository.sumTotalOutstandingAmount(schoolId));
        BigDecimal totalOverdue = MoneyUtils.scale(paymentScheduleRepository.sumTotalOverdueAmount(schoolId));

        long overdueSchedulesCount = paymentScheduleRepository.countBySchool_IdAndStatusAndDeletedFalse(
                schoolId, PaymentScheduleStatus.OVERDUE);

        // Date intervals for today and this month
        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = today.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        YearMonth currentYearMonth = YearMonth.now();
        Instant startOfMonth = currentYearMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfMonth = currentYearMonth.atEndOfMonth().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        long paymentsTodayCount = paymentRepository.countSuccessfulPaymentsBetween(schoolId, startOfDay, endOfDay);
        BigDecimal paymentsTodayAmount = MoneyUtils.scale(paymentRepository.sumSuccessfulPaymentsBetween(schoolId, startOfDay, endOfDay));
        BigDecimal paymentsThisMonthAmount = MoneyUtils.scale(paymentRepository.sumSuccessfulPaymentsBetween(schoolId, startOfMonth, endOfMonth));

        // Collection rate calculation
        BigDecimal collectionRate = BigDecimal.ZERO;
        if (MoneyUtils.isPositive(totalExpected)) {
            collectionRate = totalCollected.multiply(BigDecimal.valueOf(100))
                    .divide(totalExpected, 2, RoundingMode.HALF_UP);
        }

        return DashboardStatsDto.builder()
                .totalStudents(totalStudents)
                .totalExpectedAmount(totalExpected)
                .totalCollectedAmount(totalCollected)
                .totalOutstandingAmount(totalOutstanding)
                .totalOverdueAmount(totalOverdue)
                .paymentsTodayCount(paymentsTodayCount)
                .paymentsTodayAmount(paymentsTodayAmount)
                .paymentsThisMonthAmount(paymentsThisMonthAmount)
                .overdueSchedulesCount(overdueSchedulesCount)
                .collectionRate(collectionRate)
                .currency(school.getCurrency())
                .build();
    }
}
