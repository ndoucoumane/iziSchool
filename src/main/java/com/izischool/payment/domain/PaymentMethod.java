package com.izischool.payment.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentMethod {
    CASH,
    BANK_TRANSFER,
    MOBILE_MONEY,
    ORANGE_MONEY,
    WAVE,
    CHEQUE,
    CREDIT_CARD,
    OTHER;

    @JsonCreator
    public static PaymentMethod fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return CASH;
        }
        String normalized = value.trim().toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("É", "E")
                .replace("È", "E");

        return switch (normalized) {
            case "CASH", "ESPECES", "ESPECE" -> CASH;
            case "ORANGE_MONEY", "ORANGEMONEY", "OM", "ORANGE_MONET", "ORANGEMONET" -> ORANGE_MONEY;
            case "WAVE" -> WAVE;
            case "MOBILE_MONEY", "MOBILEMONEY", "MM", "MOBILE" -> MOBILE_MONEY;
            case "BANK_TRANSFER", "VIREMENT", "VIREMENT_BANCAIRE", "TRANSFER" -> BANK_TRANSFER;
            case "CHEQUE", "CHECK" -> CHEQUE;
            case "CREDIT_CARD", "CARTE", "CARTE_BANCAIRE", "CARD" -> CREDIT_CARD;
            case "OTHER", "AUTRE" -> OTHER;
            default -> {
                for (PaymentMethod pm : values()) {
                    if (pm.name().equalsIgnoreCase(normalized)) yield pm;
                }
                yield OTHER;
            }
        };
    }
}
