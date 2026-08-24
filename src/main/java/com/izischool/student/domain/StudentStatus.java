package com.izischool.student.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StudentStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    TRANSFERRED,
    GRADUATED,
    DROPPED_OUT,
    WITHDRAWN;

    @JsonCreator
    public static StudentStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ACTIVE;
        }
        String normalized = value.trim().toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("É", "E")
                .replace("È", "E");

        return switch (normalized) {
            case "ACTIVE", "ACTIF", "INSCRIT" -> ACTIVE;
            case "INACTIVE", "INACTIF" -> INACTIVE;
            case "SUSPENDED", "SUSPENDU", "EXCLU" -> SUSPENDED;
            case "TRANSFERRED", "TRANSFERE", "MUTATION" -> TRANSFERRED;
            case "GRADUATED", "DIPLOME", "REUSSI" -> GRADUATED;
            case "DROPPED_OUT", "DROPPEDOUT", "ABANDON", "ABANDONNE", "DECROCHAGE" -> DROPPED_OUT;
            case "WITHDRAWN", "RADIE", "RADIE_DE_LECOLE", "DEMISSION" -> WITHDRAWN;
            default -> {
                for (StudentStatus ss : values()) {
                    if (ss.name().equalsIgnoreCase(normalized)) yield ss;
                }
                yield ACTIVE;
            }
        };
    }
}
