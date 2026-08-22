package com.izischool.payment.service;

import com.izischool.audit.domain.AuditAction;
import com.izischool.audit.service.AuditService;
import com.izischool.common.exception.PaymentException;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.common.util.MoneyUtils;
import com.izischool.common.util.ReferenceGenerator;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.notification.domain.Notification;
import com.izischool.notification.domain.NotificationChannel;
import com.izischool.notification.domain.NotificationStatus;
import com.izischool.notification.domain.NotificationType;
import com.izischool.notification.service.NotificationService;
import com.izischool.parent.domain.StudentParent;
import com.izischool.parent.repository.StudentParentRepository;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentAllocation;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import com.izischool.payment.domain.Receipt;
import com.izischool.payment.repository.PaymentAllocationRepository;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.student.domain.Student;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final StudentParentRepository studentParentRepository;
    private final ReceiptService receiptService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final TenantValidationService tenantValidationService;

    /**
     * Processes a payment with automatic allocation across pending schedules.
     */
    @Transactional
    public Payment processPayment(Payment payment) {
        return processPaymentWithAllocations(payment, null);
    }

    /**
     * Core transactional payment processing with idempotency and schedule allocations.
     */
    @Transactional
    public Payment processPaymentWithAllocations(Payment payment, Map<UUID, BigDecimal> explicitAllocations) {
        tenantValidationService.validateEntityAccess(payment);
        payment.setAmount(MoneyUtils.scale(payment.getAmount()));

        if (!MoneyUtils.isPositive(payment.getAmount())) {
            throw new PaymentException("Payment amount must be strictly positive");
        }

        // 1. Idempotency Check
        if (payment.getProviderTransactionId() != null && !payment.getProviderTransactionId().isBlank()) {
            Optional<Payment> existingTx = paymentRepository.findByProviderAndProviderTransactionId(
                    payment.getProvider(), payment.getProviderTransactionId());
            if (existingTx.isPresent()) {
                Payment existing = existingTx.get();
                if (existing.getStatus() == PaymentStatus.SUCCESS) {
                    log.info("Idempotent webhook: Payment already processed for txId={}", payment.getProviderTransactionId());
                    return existing;
                }
            }
        }

        // 2. Set Reference & Status
        if (payment.getPaymentReference() == null || payment.getPaymentReference().isBlank()) {
            payment.setPaymentReference(ReferenceGenerator.generatePaymentReference(payment.getSchool().getCode()));
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(Instant.now());
        }

        Payment savedPayment = paymentRepository.save(payment);

        // 3. Process Allocations
        List<PaymentAllocation> allocations = new ArrayList<>();
        LocalDate today = LocalDate.now();

        if (explicitAllocations != null && !explicitAllocations.isEmpty()) {
            // Allocate according to explicit map
            BigDecimal totalAllocated = BigDecimal.ZERO;
            for (Map.Entry<UUID, BigDecimal> entry : explicitAllocations.entrySet()) {
                UUID scheduleId = entry.getKey();
                BigDecimal allocatedAmount = MoneyUtils.scale(entry.getValue());

                if (!MoneyUtils.isPositive(allocatedAmount)) {
                    continue;
                }

                PaymentSchedule schedule = paymentScheduleRepository.findByIdAndSchool_IdAndDeletedFalse(
                        scheduleId, savedPayment.getSchool().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("PaymentSchedule", scheduleId));

                if (schedule.getStatus() == PaymentScheduleStatus.PAID) {
                    throw new PaymentException(String.format("Schedule [%s] is already fully paid", scheduleId));
                }

                if (allocatedAmount.compareTo(schedule.getRemainingAmount()) > 0) {
                    throw new PaymentException(String.format(
                            "Allocated amount (%s) exceeds schedule remaining amount (%s)",
                            allocatedAmount, schedule.getRemainingAmount()));
                }

                schedule.setAmountPaid(schedule.getAmountPaid().add(allocatedAmount));
                schedule.recalculateStatus(today);
                paymentScheduleRepository.save(schedule);

                PaymentAllocation allocation = PaymentAllocation.builder()
                        .payment(savedPayment)
                        .paymentSchedule(schedule)
                        .amount(allocatedAmount)
                        .build();

                allocations.add(allocation);
                totalAllocated = totalAllocated.add(allocatedAmount);
            }

            if (totalAllocated.compareTo(savedPayment.getAmount()) != 0) {
                throw new PaymentException(String.format(
                        "Total allocated (%s) does not match payment amount (%s)",
                        totalAllocated, savedPayment.getAmount()));
            }
        } else {
            // Automatic FIFO allocation over pending/overdue schedules for this student
            List<PaymentSchedule> pendingSchedules = paymentScheduleRepository
                    .findBySchool_IdAndStudent_IdAndStatusInAndDeletedFalseOrderByDueDateAsc(
                            savedPayment.getSchool().getId(),
                            savedPayment.getStudent().getId(),
                            List.of(PaymentScheduleStatus.PENDING, PaymentScheduleStatus.PARTIALLY_PAID, PaymentScheduleStatus.OVERDUE));

            BigDecimal unallocatedAmount = savedPayment.getAmount();

            for (PaymentSchedule schedule : pendingSchedules) {
                if (MoneyUtils.isZero(unallocatedAmount)) {
                    break;
                }

                BigDecimal needed = schedule.getRemainingAmount();
                BigDecimal toAllocate = unallocatedAmount.min(needed);

                schedule.setAmountPaid(schedule.getAmountPaid().add(toAllocate));
                schedule.recalculateStatus(today);
                paymentScheduleRepository.save(schedule);

                PaymentAllocation allocation = PaymentAllocation.builder()
                        .payment(savedPayment)
                        .paymentSchedule(schedule)
                        .amount(toAllocate)
                        .build();

                allocations.add(allocation);
                unallocatedAmount = unallocatedAmount.subtract(toAllocate);
            }

            if (MoneyUtils.isPositive(unallocatedAmount)) {
                // Payment exceeds total unpaid schedules
                log.warn("Payment {} of {} exceeds total pending schedules by {}",
                        savedPayment.getPaymentReference(), savedPayment.getAmount(), unallocatedAmount);
                throw new PaymentException(String.format(
                        "Payment amount [%s] exceeds total pending balance by [%s]",
                        savedPayment.getAmount(), unallocatedAmount));
            }
        }

        paymentAllocationRepository.saveAll(allocations);

        // 4. Generate Receipt
        Receipt receipt = receiptService.generateReceipt(savedPayment);

        // 5. Audit sensitive financial operation
        String entityId = savedPayment.getId() != null ? savedPayment.getId().toString() : savedPayment.getPaymentReference();
        auditService.logAction(
                savedPayment.getSchool(),
                null,
                AuditAction.CREATE,
                "Payment",
                entityId,
                null,
                String.format("Payment %s created with amount %s %s, receipt %s",
                        savedPayment.getPaymentReference(), savedPayment.getAmount(),
                        savedPayment.getCurrency(), receipt.getReceiptNumber()),
                null,
                null
        );

        // 6. Dispatch Notification
        sendPaymentNotification(savedPayment, receipt);

        log.info("Successfully processed payment {} with {} allocations",
                savedPayment.getPaymentReference(), allocations.size());
        return savedPayment;
    }

    private void sendPaymentNotification(Payment payment, Receipt receipt) {
        try {
            Student student = payment.getStudent();
            Optional<StudentParent> financialContactOpt = studentParentRepository.findFinancialContactByStudentId(student.getId());

            String recipientPhone = payment.getParent() != null ? payment.getParent().getPhone() :
                    (financialContactOpt.isPresent() ? financialContactOpt.get().getParent().getPhone() : null);

            String recipientEmail = payment.getParent() != null ? payment.getParent().getEmail() :
                    (financialContactOpt.isPresent() ? financialContactOpt.get().getParent().getEmail() : null);

            String parentName = payment.getParent() != null ? payment.getParent().getFullName() :
                    (financialContactOpt.isPresent() ? financialContactOpt.get().getParent().getFullName() : "Parent");

            String message = String.format("Cher %s, nous confirmons la reception du paiement de %s %s pour l'eleve %s. Recu N° %s.",
                    parentName, payment.getAmount(), payment.getCurrency(), student.getFullName(), receipt.getReceiptNumber());

            Notification notification = Notification.builder()
                    .school(payment.getSchool())
                    .student(student)
                    .recipientPhone(recipientPhone)
                    .recipientEmail(recipientEmail)
                    .type(NotificationType.PAYMENT_CONFIRMATION)
                    .channel(recipientPhone != null ? NotificationChannel.SMS : NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .title("Confirmation de paiement iziSchool")
                    .message(message)
                    .build();

            notificationService.createNotification(notification);
        } catch (Exception e) {
            log.warn("Failed to dispatch payment notification for payment {}: {}", payment.getPaymentReference(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return paymentRepository.findByIdAndSchool_IdAndDeletedFalse(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPaymentsBySchool(UUID schoolId, Pageable pageable) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return paymentRepository.findBySchool_IdAndDeletedFalse(schoolId, pageable);
    }
}
