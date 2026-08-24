package com.izischool.payment.dto;

import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private String paymentReference;
    private String providerTransactionId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentProvider provider;
    private PaymentStatus status;
    private Instant paymentDate;
    private UUID studentId;
    private String studentName;
    private String studentNumber;
    private UUID parentId;
    private String parentName;
    private String description;
    private Instant createdAt;

    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }

        UUID studentId = null;
        String studentName = null;
        String studentNumber = null;
        if (payment.getStudent() != null) {
            try {
                studentId = payment.getStudent().getId();
                studentName = payment.getStudent().getFullName();
                studentNumber = payment.getStudent().getStudentNumber();
            } catch (Exception ignored) {
            }
        }

        UUID parentId = null;
        String parentName = null;
        if (payment.getParent() != null) {
            try {
                parentId = payment.getParent().getId();
                parentName = payment.getParent().getFullName();
            } catch (Exception ignored) {
            }
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .providerTransactionId(payment.getProviderTransactionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .provider(payment.getProvider())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .studentId(studentId)
                .studentName(studentName)
                .studentNumber(studentNumber)
                .parentId(parentId)
                .parentName(parentName)
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
