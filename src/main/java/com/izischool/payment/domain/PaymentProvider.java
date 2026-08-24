package com.izischool.payment.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentProvider {
    MANUAL,
    WAVE,
    ORANGE_MONEY,
    OTHER;

    @JsonCreator
    public static PaymentProvider fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MANUAL;
        }
        String normalized = value.trim().toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("É", "E")
                .replace("È", "E");

        return switch (normalized) {
            case "MANUAL", "MANUEL", "CASH", "ESPECES" -> MANUAL;
            case "WAVE" -> WAVE;
            case "ORANGE_MONEY", "ORANGEMONEY", "OM", "ORANGE_MONET", "ORANGEMONET" -> ORANGE_MONEY;
            default -> {
                for (PaymentProvider pp : values()) {
                    if (pp.name().equalsIgnoreCase(normalized)) yield pp;
                }
                yield OTHER;
            }
        };
    }
}
