package com.izischool.payment.service;

import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.common.util.ReferenceGenerator;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.Receipt;
import com.izischool.payment.domain.ReceiptStatus;
import com.izischool.payment.repository.ReceiptRepository;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public Receipt generateReceipt(Payment payment) {
        tenantValidationService.validateEntityAccess(payment);
        UUID schoolId = payment.getSchool().getId();

        // Check if receipt already exists for this payment
        if (payment.getId() != null) {
            Optional<Receipt> existing = receiptRepository.findByPayment_Id(payment.getId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        long sequence = receiptRepository.countBySchool_Id(schoolId) + 1;
        String schoolCode = payment.getSchool().getCode();
        String receiptNumber;
        do {
            receiptNumber = ReferenceGenerator.generateReceiptNumber(schoolCode, sequence++);
        } while (receiptRepository.existsBySchool_IdAndReceiptNumber(schoolId, receiptNumber));

        Receipt receipt = Receipt.builder()
                .school(payment.getSchool())
                .payment(payment)
                .receiptNumber(receiptNumber)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .issuedAt(Instant.now())
                .status(ReceiptStatus.GENERATED)
                .build();

        Receipt saved = receiptRepository.save(receipt);
        log.info("Generated receipt {} for payment {}", receiptNumber, payment.getPaymentReference());
        return saved;
    }

    @Transactional(readOnly = true)
    public Receipt getReceiptById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return receiptRepository.findByIdAndSchool_IdAndDeletedFalse(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", id));
    }

    @Transactional(readOnly = true)
    public Receipt getReceiptByIdOrNumber(String identifier, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        try {
            UUID id = UUID.fromString(identifier);
            Optional<Receipt> byId = receiptRepository.findByIdAndSchool_IdAndDeletedFalse(id, schoolId);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (IllegalArgumentException ignored) {
        }

        return receiptRepository.findBySchool_IdAndReceiptNumber(schoolId, identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", identifier));
    }

    @Transactional(readOnly = true)
    public Receipt getReceiptByPaymentId(UUID paymentId) {
        return receiptRepository.findByPayment_Id(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt for payment", paymentId));
    }

    @Transactional(readOnly = true)
    public Page<Receipt> getReceiptsBySchool(UUID schoolId, Pageable pageable) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return receiptRepository.findBySchool_IdAndDeletedFalse(schoolId, pageable);
    }
}
