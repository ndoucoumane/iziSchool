package com.izischool.finance.service;

import com.izischool.common.exception.BusinessException;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.common.util.MoneyUtils;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.PaymentFrequency;
import com.izischool.finance.domain.PaymentPlan;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.student.domain.StudentEnrollment;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentScheduleService {

    private final PaymentScheduleRepository paymentScheduleRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public PaymentSchedule createSchedule(PaymentSchedule schedule) {
        tenantValidationService.validateEntityAccess(schedule);

        schedule.setAmountDue(MoneyUtils.scale(schedule.getAmountDue()));
        schedule.setAmountPaid(MoneyUtils.scale(schedule.getAmountPaid()));
        schedule.setRemainingAmount(MoneyUtils.subtract(schedule.getAmountDue(), schedule.getAmountPaid()));

        if (schedule.getStatus() == null) {
            schedule.recalculateStatus(LocalDate.now());
        }

        return paymentScheduleRepository.save(schedule);
    }

    /**
     * Generates a series of PaymentSchedules for an enrollment based on a PaymentPlan and total amount.
     */
    @Transactional
    public List<PaymentSchedule> generateSchedulesFromPlan(
            StudentEnrollment enrollment,
            Fee fee,
            PaymentPlan plan,
            BigDecimal totalAmount,
            LocalDate firstDueDate
    ) {
        tenantValidationService.validateEntityAccess(enrollment);
        int installments = plan.getNumberOfInstallments();
        if (installments <= 0) {
            throw new BusinessException("Payment plan must have at least 1 installment");
        }

        totalAmount = MoneyUtils.scale(totalAmount);
        BigDecimal installmentAmount = totalAmount.divide(BigDecimal.valueOf(installments), 2, RoundingMode.FLOOR);
        BigDecimal totalDistributed = installmentAmount.multiply(BigDecimal.valueOf(installments));
        BigDecimal remainder = totalAmount.subtract(totalDistributed);

        List<PaymentSchedule> schedules = new ArrayList<>();
        LocalDate currentDueDate = firstDueDate != null ? firstDueDate : enrollment.getAcademicYear().getStartDate();

        for (int i = 1; i <= installments; i++) {
            BigDecimal currentAmount = installmentAmount;
            // Add remainder centimes to the first installment
            if (i == 1 && MoneyUtils.isPositive(remainder)) {
                currentAmount = currentAmount.add(remainder);
            }

            PaymentSchedule schedule = PaymentSchedule.builder()
                    .school(enrollment.getSchool())
                    .student(enrollment.getStudent())
                    .enrollment(enrollment)
                    .fee(fee)
                    .dueDate(currentDueDate)
                    .amountDue(currentAmount)
                    .amountPaid(BigDecimal.ZERO)
                    .remainingAmount(currentAmount)
                    .currency(fee.getCurrency())
                    .status(PaymentScheduleStatus.PENDING)
                    .installmentNumber(i)
                    .description(String.format("%s - Échéance %d/%d", fee.getName(), i, installments))
                    .build();

            schedule.recalculateStatus(LocalDate.now());
            schedules.add(schedule);

            // Compute next due date according to frequency
            currentDueDate = computeNextDueDate(currentDueDate, plan.getFrequency());
        }

        List<PaymentSchedule> savedSchedules = paymentScheduleRepository.saveAll(schedules);
        log.info("Generated {} payment schedules for student {} and fee {}",
                savedSchedules.size(), enrollment.getStudent().getStudentNumber(), fee.getCode());
        return savedSchedules;
    }

    private LocalDate computeNextDueDate(LocalDate date, PaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case SEMESTER -> date.plusMonths(6);
            case ONCE, CUSTOM -> date;
        };
    }

    @Transactional(readOnly = true)
    public PaymentSchedule getScheduleById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return paymentScheduleRepository.findByIdAndSchool_IdAndDeletedFalse(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentSchedule", id));
    }

    @Transactional(readOnly = true)
    public List<PaymentSchedule> getSchedulesByStudent(UUID schoolId, UUID studentId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return paymentScheduleRepository.findBySchool_IdAndStudent_IdAndDeletedFalseOrderByDueDateAsc(schoolId, studentId);
    }

    @Transactional(readOnly = true)
    public List<PaymentSchedule> getPendingSchedulesByStudent(UUID schoolId, UUID studentId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return paymentScheduleRepository.findBySchool_IdAndStudent_IdAndStatusInAndDeletedFalseOrderByDueDateAsc(
                schoolId, studentId, List.of(PaymentScheduleStatus.PENDING, PaymentScheduleStatus.PARTIALLY_PAID, PaymentScheduleStatus.OVERDUE));
    }

    @Transactional
    public void updateOverdueStatuses(UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        LocalDate today = LocalDate.now();
        List<PaymentSchedule> overdueList = paymentScheduleRepository.findOverdueSchedules(schoolId, today);
        for (PaymentSchedule schedule : overdueList) {
            schedule.setStatus(PaymentScheduleStatus.OVERDUE);
            paymentScheduleRepository.save(schedule);
        }
        log.info("Updated {} schedules to OVERDUE for school {}", overdueList.size(), schoolId);
    }
}
