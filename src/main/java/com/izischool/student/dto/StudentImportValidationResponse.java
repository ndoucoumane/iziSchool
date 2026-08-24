package com.izischool.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportValidationResponse {

    private UUID importId;
    private int totalRows;
    private int validRows;
    private int invalidRows;

    @Builder.Default
    private List<ImportRowError> errors = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportRowError {
        private int row;
        private String field;
        private String message;
    }
}
