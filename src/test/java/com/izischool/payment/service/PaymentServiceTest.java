package com.izischool.payment.service;

import com.izischool.audit.service.AuditService;
import com.izischool.common.exception.PaymentException;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.notification.service.NotificationService;
import com.izischool.parent.repository.StudentParentRepository;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import com.izischool.payment.domain.Receipt;
import com.izischool.payment.domain.ReceiptStatus;
import com.izischool.payment.repository.PaymentAllocationRepository;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentStatus;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Mock
    private PaymentScheduleRepository paymentScheduleRepository;

    @Mock
    private StudentParentRepository studentParentRepository;

    @Mock
    private ReceiptService receiptService;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TenantValidationService tenantValidationService;

    @InjectMocks
    private PaymentService paymentService;

    private School school;
    private Student student;
    private PaymentSchedule schedule;

    @BeforeEach
    void setUp() {
        school = School.builder()
                .id(UUID.randomUUID())
                .name("École d'Excellence")
                .code("EXCELLENCE")
                .currency("XOF")
                .status(SchoolStatus.ACTIVE)
                .build();

        student = Student.builder()
                .id(UUID.randomUUID())
                .school(school)
                .studentNumber("EXC-STD-00001")
                .firstName("Amadou")
                .lastName("Diallo")
                .gender(Gender.MALE)
                .status(StudentStatus.ACTIVE)
                .build();

        schedule = PaymentSchedule.builder()
                .id(UUID.randomUUID())
                .school(school)
                .student(student)
                .dueDate(LocalDate.now().plusDays(10))
                .amountDue(new BigDecimal("75000.00"))
                .amountPaid(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("75000.00"))
                .currency("XOF")
                .status(PaymentScheduleStatus.PENDING)
                .installmentNumber(1)
                .build();
    }

    @Test
    @DisplayName("Cas 1 : Paiement exact (75 000 FCFA) -> statut PAID, remainingAmount = 0")
    void testExactPayment() {
        Payment payment = Payment.builder()
                .school(school)
                .student(student)
                .amount(new BigDecimal("75000.00"))
                .currency("XOF")
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .provider(PaymentProvider.WAVE)
                .providerTransactionId("WAVE-TX-1001")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByProviderAndProviderTransactionId(PaymentProvider.WAVE, "WAVE-TX-1001"))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(paymentScheduleRepository.findBySchool_IdAndStudent_IdAndStatusInAndDeletedFalseOrderByDueDateAsc(
                eq(school.getId()), eq(student.getId()), anyList()))
                .thenReturn(List.of(schedule));

        Receipt receipt = Receipt.builder()
                .id(UUID.randomUUID())
                .receiptNumber("EXC-REC-2026-000001")
                .amount(payment.getAmount())
                .status(ReceiptStatus.GENERATED)
                .build();
        when(receiptService.generateReceipt(any(Payment.class))).thenReturn(receipt);

        Payment result = paymentService.processPayment(payment);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(schedule.getAmountPaid()).isEqualByComparingTo("75000.00");
        assertThat(schedule.getRemainingAmount()).isEqualByComparingTo("0.00");
        assertThat(schedule.getStatus()).isEqualTo(PaymentScheduleStatus.PAID);

        verify(paymentAllocationRepository, times(1)).saveAll(anyList());
        verify(receiptService, times(1)).generateReceipt(any(Payment.class));
    }

    @Test
    @DisplayName("Cas 2 : Paiement partiel (50 000 sur 75 000 FCFA) -> statut PARTIALLY_PAID, remainingAmount = 25 000")
    void testPartialPayment() {
        Payment payment = Payment.builder()
                .school(school)
                .student(student)
                .amount(new BigDecimal("50000.00"))
                .currency("XOF")
                .paymentMethod(PaymentMethod.CASH)
                .provider(PaymentProvider.MANUAL)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(paymentScheduleRepository.findBySchool_IdAndStudent_IdAndStatusInAndDeletedFalseOrderByDueDateAsc(
                eq(school.getId()), eq(student.getId()), anyList()))
                .thenReturn(List.of(schedule));

        Receipt receipt = Receipt.builder()
                .id(UUID.randomUUID())
                .receiptNumber("EXC-REC-2026-000002")
                .amount(payment.getAmount())
                .status(ReceiptStatus.GENERATED)
                .build();
        when(receiptService.generateReceipt(any(Payment.class))).thenReturn(receipt);

        Payment result = paymentService.processPayment(payment);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(schedule.getAmountPaid()).isEqualByComparingTo("50000.00");
        assertThat(schedule.getRemainingAmount()).isEqualByComparingTo("25000.00");
        assertThat(schedule.getStatus()).isEqualTo(PaymentScheduleStatus.PARTIALLY_PAID);

        verify(paymentAllocationRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Cas 3 : Surpaiement (100 000 FCFA pour une dette de 75 000 FCFA) -> lève PaymentException")
    void testOverpaymentRejection() {
        Payment payment = Payment.builder()
                .school(school)
                .student(student)
                .amount(new BigDecimal("100000.00"))
                .currency("XOF")
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .provider(PaymentProvider.ORANGE_MONEY)
                .providerTransactionId("OM-TX-5555")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByProviderAndProviderTransactionId(PaymentProvider.ORANGE_MONEY, "OM-TX-5555"))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentScheduleRepository.findBySchool_IdAndStudent_IdAndStatusInAndDeletedFalseOrderByDueDateAsc(
                eq(school.getId()), eq(student.getId()), anyList()))
                .thenReturn(List.of(schedule));

        assertThatThrownBy(() -> paymentService.processPayment(payment))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("exceeds total pending balance");

        verify(receiptService, never()).generateReceipt(any());
    }

    @Test
    @DisplayName("Cas 4 : Idempotence webhook (même transaction reçue 2 fois) -> retourne le paiement sans duplication")
    void testIdempotentDuplicateWebhook() {
        Payment existingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .school(school)
                .student(student)
                .paymentReference("EXC-PAY-20260822-ABC123")
                .amount(new BigDecimal("75000.00"))
                .currency("XOF")
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .provider(PaymentProvider.WAVE)
                .providerTransactionId("WAVE-TX-DUPLICATE")
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment incomingWebhookPayment = Payment.builder()
                .school(school)
                .student(student)
                .amount(new BigDecimal("75000.00"))
                .currency("XOF")
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .provider(PaymentProvider.WAVE)
                .providerTransactionId("WAVE-TX-DUPLICATE")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByProviderAndProviderTransactionId(PaymentProvider.WAVE, "WAVE-TX-DUPLICATE"))
                .thenReturn(Optional.of(existingPayment));

        Payment result = paymentService.processPayment(incomingWebhookPayment);

        assertThat(result).isSameAs(existingPayment);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        verify(paymentRepository, never()).save(any());
        verify(paymentAllocationRepository, never()).saveAll(any());
        verify(receiptService, never()).generateReceipt(any());
    }
}
