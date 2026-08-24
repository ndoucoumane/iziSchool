package com.izischool.student.dto;

import com.izischool.common.util.MoneyUtils;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {

    private UUID id;
    private String studentNumber;
    private String firstName;
    private String lastName;
    private String middleName;
    private String fullName;
    private LocalDate dateOfBirth;
    private String placeOfBirth;
    private Gender gender;
    private String photoUrl;
    private StudentStatus status;
    private UUID schoolId;

    // Champs de scolarité & classe
    private UUID classId;
    private String className;

    // Champs financiers
    private BigDecimal totalDue;
    private BigDecimal totalPaid;
    private BigDecimal remainingAmount;
    private String paymentStatus; // PAID, PARTIALLY_PAID, UNPAID, PENDING

    private Instant createdAt;
    private Instant updatedAt;

    public static StudentResponse fromEntity(Student student) {
        if (student == null) {
            return null;
        }
        return StudentResponse.builder()
                .id(student.getId())
                .studentNumber(student.getStudentNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .middleName(student.getMiddleName())
                .fullName(student.getFullName())
                .dateOfBirth(student.getDateOfBirth())
                .placeOfBirth(student.getPlaceOfBirth())
                .gender(student.getGender())
                .photoUrl(student.getPhotoUrl())
                .status(student.getStatus())
                .schoolId(student.getSchool() != null ? student.getSchool().getId() : null)
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }

    public static StudentResponse fromEntity(
            Student student,
            UUID classId,
            String className,
            BigDecimal totalDue,
            BigDecimal totalPaid,
            BigDecimal remainingAmount,
            String paymentStatus) {
        if (student == null) {
            return null;
        }
        return StudentResponse.builder()
                .id(student.getId())
                .studentNumber(student.getStudentNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .middleName(student.getMiddleName())
                .fullName(student.getFullName())
                .dateOfBirth(student.getDateOfBirth())
                .placeOfBirth(student.getPlaceOfBirth())
                .gender(student.getGender())
                .photoUrl(student.getPhotoUrl())
                .status(student.getStatus())
                .schoolId(student.getSchool() != null ? student.getSchool().getId() : null)
                .classId(classId)
                .className(className)
                .totalDue(MoneyUtils.scale(totalDue))
                .totalPaid(MoneyUtils.scale(totalPaid))
                .remainingAmount(MoneyUtils.scale(remainingAmount))
                .paymentStatus(paymentStatus != null ? paymentStatus : "PENDING")
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }
}
