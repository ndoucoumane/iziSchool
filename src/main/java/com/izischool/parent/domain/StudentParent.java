package com.izischool.parent.domain;

import com.izischool.common.entity.BaseEntity;
import com.izischool.student.domain.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "student_parents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_parent_pair", columnNames = {"student_id", "parent_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentParent extends BaseEntity {

    @NotNull(message = "Student is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull(message = "Parent is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @NotNull(message = "Relationship type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false, length = 50)
    private ParentRelationship relationship;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @Builder.Default
    @Column(name = "is_financial_contact", nullable = false)
    private boolean isFinancialContact = false;

    @Builder.Default
    @Column(name = "is_emergency_contact", nullable = false)
    private boolean isEmergencyContact = false;
}
