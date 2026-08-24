package com.izischool.dashboard.service;

import com.izischool.common.util.MoneyUtils;
import com.izischool.dashboard.dto.ArrearsReportResponse;
import com.izischool.dashboard.dto.DashboardOverviewResponse;
import com.izischool.dashboard.dto.DashboardStatsDto;
import com.izischool.dashboard.dto.RevenueTrendItem;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentStatus;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = today.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        YearMonth currentYearMonth = YearMonth.now();
        Instant startOfMonth = currentYearMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfMonth = currentYearMonth.atEndOfMonth().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        long paymentsTodayCount = paymentRepository.countSuccessfulPaymentsBetween(schoolId, startOfDay, endOfDay);
        BigDecimal paymentsTodayAmount = MoneyUtils.scale(paymentRepository.sumSuccessfulPaymentsBetween(schoolId, startOfDay, endOfDay));
        BigDecimal paymentsThisMonthAmount = MoneyUtils.scale(paymentRepository.sumSuccessfulPaymentsBetween(schoolId, startOfMonth, endOfMonth));

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

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        School school = schoolService.getSchoolById(schoolId);

        long totalStudents = studentRepository.countBySchool_IdAndDeletedFalse(schoolId);
        BigDecimal totalExpected = MoneyUtils.scale(paymentScheduleRepository.sumTotalExpectedAmount(schoolId));
        BigDecimal totalCollected = MoneyUtils.scale(paymentScheduleRepository.sumTotalCollectedAmount(schoolId));
        BigDecimal totalOutstanding = MoneyUtils.scale(paymentScheduleRepository.sumTotalOutstandingAmount(schoolId));

        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = today.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        BigDecimal paymentsTodayAmount = MoneyUtils.scale(paymentRepository.sumSuccessfulPaymentsBetween(schoolId, startOfDay, endOfDay));

        long overdueSchedulesCount = paymentScheduleRepository.countBySchool_IdAndStatusAndDeletedFalse(
                schoolId, PaymentScheduleStatus.OVERDUE);

        BigDecimal collectionRate = BigDecimal.ZERO;
        if (MoneyUtils.isPositive(totalExpected)) {
            collectionRate = totalCollected.multiply(BigDecimal.valueOf(100))
                    .divide(totalExpected, 2, RoundingMode.HALF_UP);
        }

        return DashboardOverviewResponse.builder()
                .students(totalStudents)
                .totalExpected(totalExpected)
                .totalCollected(totalCollected)
                .totalOutstanding(totalOutstanding)
                .collectionRate(collectionRate)
                .todayPayments(paymentsTodayAmount)
                .overdueSchedules(overdueSchedulesCount)
                .currency(school.getCurrency())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RevenueTrendItem> getRevenueTrend(UUID schoolId, int days) {
        tenantValidationService.validateSchoolAccess(schoolId);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days > 0 ? days : 7);

        Map<LocalDate, BigDecimal> dailyTotals = new TreeMap<>();
        for (int i = 0; i <= (days > 0 ? days : 7); i++) {
            dailyTotals.put(startDate.plusDays(i), BigDecimal.ZERO);
        }

        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> p.getSchool().getId().equals(schoolId))
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .filter(p -> p.getPaymentDate() != null)
                .toList();

        for (Payment payment : payments) {
            LocalDate pDate = payment.getPaymentDate().atZone(ZoneId.systemDefault()).toLocalDate();
            if (dailyTotals.containsKey(pDate)) {
                dailyTotals.put(pDate, dailyTotals.get(pDate).add(payment.getAmount()));
            }
        }

        List<RevenueTrendItem> trend = new ArrayList<>();
        dailyTotals.forEach((date, amount) -> trend.add(new RevenueTrendItem(date, MoneyUtils.scale(amount))));
        return trend;
    }

    @Transactional(readOnly = true)
    public ArrearsReportResponse getArrearsReport(UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        School school = schoolService.getSchoolById(schoolId);
        LocalDate today = LocalDate.now();

        List<PaymentSchedule> overdueSchedules = paymentScheduleRepository.findOverdueSchedules(schoolId, today);

        BigDecimal totalOverdueAmount = BigDecimal.ZERO;
        BigDecimal bucket0To7 = BigDecimal.ZERO;
        BigDecimal bucket8To30 = BigDecimal.ZERO;
        BigDecimal bucket30Plus = BigDecimal.ZERO;

        for (PaymentSchedule schedule : overdueSchedules) {
            BigDecimal remaining = schedule.getRemainingAmount();
            totalOverdueAmount = totalOverdueAmount.add(remaining);

            long daysLate = ChronoUnit.DAYS.between(schedule.getDueDate(), today);
            if (daysLate <= 7) {
                bucket0To7 = bucket0To7.add(remaining);
            } else if (daysLate <= 30) {
                bucket8To30 = bucket8To30.add(remaining);
            } else {
                bucket30Plus = bucket30Plus.add(remaining);
            }
        }

        return ArrearsReportResponse.builder()
                .totalOverdue(overdueSchedules.size())
                .overdueAmount(MoneyUtils.scale(totalOverdueAmount))
                .bucket0To7Days(MoneyUtils.scale(bucket0To7))
                .bucket8To30Days(MoneyUtils.scale(bucket8To30))
                .bucket30PlusDays(MoneyUtils.scale(bucket30Plus))
                .currency(school.getCurrency())
                .build();
    }
}
