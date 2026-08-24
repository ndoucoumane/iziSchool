package com.izischool.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentProvider;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Student ID is required")
    @JsonAlias({"student_id", "eleveId", "idEleve"})
    private UUID studentId;

    @JsonAlias({"schedule_id", "echeanceId", "echeancierId"})
    private UUID scheduleId;

    @JsonAlias({"parent_id", "tuteurId"})
    private UUID parentId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be strictly positive")
    @JsonAlias({"montant", "montantVerse", "amountPaid"})
    private BigDecimal amount;

    @Builder.Default
    @NotNull(message = "Payment method is required (CASH, BANK_TRANSFER, MOBILE_MONEY, ORANGE_MONEY, WAVE, CHEQUE, CREDIT_CARD, OTHER)")
    @JsonAlias({"modePaiement", "mode_paiement", "moyenPaiement", "moyen_paiement", "payment_method", "typePaiement"})
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @JsonAlias({"providerName", "fournisseur"})
    private PaymentProvider provider;

    @JsonAlias({"referencePaiement", "reference_paiement", "ref", "txId", "transactionId"})
    private String reference;

    @JsonAlias({"description", "commentaire", "motif", "remarque"})
    private String note;
}
