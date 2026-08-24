package com.izischool.payment.dto;

import com.izischool.payment.domain.Receipt;
import com.izischool.payment.domain.ReceiptStatus;
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
public class ReceiptResponse {

    private UUID id;
    private String receiptNumber;
    private UUID paymentId;
    private String paymentReference;
    private BigDecimal amount;
    private String currency;
    private Instant issuedAt;
    private ReceiptStatus status;
    private String pdfUrl;
    private Instant createdAt;

    public static ReceiptResponse fromEntity(Receipt receipt) {
        if (receipt == null) {
            return null;
        }

        UUID paymentId = null;
        String paymentReference = null;
        if (receipt.getPayment() != null) {
            try {
                paymentId = receipt.getPayment().getId();
                paymentReference = receipt.getPayment().getPaymentReference();
            } catch (Exception ignored) {
            }
        }

        return ReceiptResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .paymentId(paymentId)
                .paymentReference(paymentReference)
                .amount(receipt.getAmount())
                .currency(receipt.getCurrency())
                .issuedAt(receipt.getIssuedAt())
                .status(receipt.getStatus())
                .pdfUrl(receipt.getPdfUrl())
                .createdAt(receipt.getCreatedAt())
                .build();
    }
}
