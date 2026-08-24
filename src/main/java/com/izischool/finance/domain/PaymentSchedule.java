package com.izischool.finance.domain;

import com.izischool.common.entity.SoftDeletableTenantAwareEntity;
import com.izischool.common.util.MoneyUtils;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentEnrollment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payment_schedules")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSchedule extends SoftDeletableTenantAwareEntity {

    @NotNull(message = "Student is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull(message = "Enrollment is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private StudentEnrollment enrollment;

    @NotNull(message = "Fee is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fee_id", nullable = false)
    private Fee fee;

    @NotNull(message = "Due date is required")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull(message = "Amount due is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount due must be positive")
    @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue;

    @NotNull
    @PositiveOrZero
    @Builder.Default
    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    @Column(name = "remaining_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingAmount;

    @NotBlank
    @Size(min = 3, max = 10)
    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentScheduleStatus status;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Size(max = 255)
    @Column(name = "description")
    private String description;

    /**
     * Recalculates remaining amount and status based on current amountPaid and currentDate.
     */
    public void recalculateStatus(LocalDate currentDate) {
        if (this.status == PaymentScheduleStatus.CANCELLED) {
            return;
        }

        this.amountDue = MoneyUtils.scale(this.amountDue);
        this.amountPaid = MoneyUtils.scale(this.amountPaid);
        this.remainingAmount = MoneyUtils.subtract(this.amountDue, this.amountPaid);

        if (MoneyUtils.isZero(this.remainingAmount)) {
            this.status = PaymentScheduleStatus.PAID;
        } else if (MoneyUtils.isPositive(this.amountPaid) && MoneyUtils.isPositive(this.remainingAmount)) {
            this.status = PaymentScheduleStatus.PARTIALLY_PAID;
        } else if (currentDate != null && currentDate.isAfter(this.dueDate)) {
            this.status = PaymentScheduleStatus.OVERDUE;
        } else {
            this.status = PaymentScheduleStatus.PENDING;
        }
    }
}
