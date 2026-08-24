package com.izischool.student.dto;

import com.izischool.payment.domain.PaymentMethod;
import com.izischool.school.domain.School;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Size(max = 100)
    private String middleName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 100)
    private String placeOfBirth;

    @NotNull(message = "Gender is required (MALE, FEMALE, OTHER)")
    private Gender gender;

    private UUID classId;

    private UUID parentId;

    @Size(max = 500)
    private String photoUrl;

    /**
     * Montant total dû (scolarité / frais d'inscription).
     */
    @PositiveOrZero(message = "Total due amount must be positive or zero")
    private BigDecimal totalDue;

    /**
     * Versement initial effectué lors de l'inscription (peut être partiel ou total).
     */
    @PositiveOrZero(message = "Initial payment amount must be positive or zero")
    private BigDecimal initialPayment;

    /**
     * Moyen de paiement pour le versement initial (CASH, MOBILE_MONEY, BANK_TRANSFER, OTHER).
     */
    private PaymentMethod paymentMethod;

    /**
     * Optionnel : Identifiant du frais / tarif à associer.
     */
    private UUID feeId;

    /**
     * Optionnel : Nombre d'échéances pour répartir le montant total dû (par défaut 1).
     */
    private Integer numberOfInstallments;

    public Student toEntity(School school) {
        return Student.builder()
                .school(school)
                .firstName(firstName)
                .lastName(lastName)
                .middleName(middleName)
                .dateOfBirth(dateOfBirth)
                .placeOfBirth(placeOfBirth)
                .gender(gender)
                .photoUrl(photoUrl)
                .status(StudentStatus.ACTIVE)
                .build();
    }
}
