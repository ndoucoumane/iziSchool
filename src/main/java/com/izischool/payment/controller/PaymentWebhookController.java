package com.izischool.payment.controller;

import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Payment Webhooks", description = "Endpoints de réception asynchrone des notifications de paiements Mobile Money (Wave, Orange Money)")
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @Operation(summary = "Webhook Wave", description = "Réception des notifications de succès/échec de paiement Wave avec idempotence")
    @ApiResponse(responseCode = "200", description = "Webhook traité avec succès")
    @PostMapping("/wave")
    public ResponseEntity<Map<String, String>> handleWaveWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Wave-Signature", required = false) String signature) {
        log.info("Received Wave Webhook event");

        String txId = (String) payload.get("id");
        String paymentReference = (String) payload.get("client_reference");

        if (txId == null && paymentReference == null) {
            return ResponseEntity.ok(Map.of("status", "IGNORED", "message", "No reference found"));
        }

        Optional<Payment> paymentOpt = paymentReference != null
                ? paymentRepository.findByPaymentReference(paymentReference)
                : paymentRepository.findByProviderAndProviderTransactionId(PaymentProvider.WAVE, txId);

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                log.info("Wave webhook: Payment {} already confirmed (idempotent)", payment.getPaymentReference());
                return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Already processed"));
            }

            payment.setProviderTransactionId(txId);
            paymentService.processPayment(payment);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Payment confirmed"));
        }

        return ResponseEntity.ok(Map.of("status", "NOT_FOUND", "message", "Transaction not matched"));
    }

    @Operation(summary = "Webhook Orange Money", description = "Réception des notifications de paiement Orange Money")
    @ApiResponse(responseCode = "200", description = "Webhook traité")
    @PostMapping("/orange-money")
    public ResponseEntity<Map<String, String>> handleOrangeMoneyWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-OM-Signature", required = false) String signature) {
        log.info("Received Orange Money Webhook event");

        String txId = (String) payload.get("txnid");
        String ref = (String) payload.get("order_id");

        if (ref != null) {
            Optional<Payment> paymentOpt = paymentRepository.findByPaymentReference(ref);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.setProviderTransactionId(txId != null ? txId : "OM-" + ref);
                    paymentService.processPayment(payment);
                }
            }
        }

        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }
}
