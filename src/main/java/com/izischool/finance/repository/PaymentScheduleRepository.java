package com.izischool.finance.repository;

import com.izischool.finance.domain.FeeType;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, UUID> {

    @EntityGraph(attributePaths = {"student", "fee"})
    Optional<PaymentSchedule> findByIdAndSchool_IdAndDeletedFalse(UUID id, UUID schoolId);

    @EntityGraph(attributePaths = {"student", "fee"})
    List<PaymentSchedule> findBySchool_IdAndStudent_IdAndDeletedFalseOrderByDueDateAsc(UUID schoolId, UUID studentId);

    @EntityGraph(attributePaths = {"student", "fee"})
    List<PaymentSchedule> findBySchool_IdAndEnrollment_IdAndDeletedFalseOrderByDueDateAsc(UUID schoolId, UUID enrollmentId);

    @EntityGraph(attributePaths = {"student", "fee"})
    List<PaymentSchedule> findBySchool_IdAndStudent_IdAndStatusInAndDeletedFalseOrderByDueDateAsc(
            UUID schoolId, UUID studentId, List<PaymentScheduleStatus> statuses);

    @EntityGraph(attributePaths = {"student", "fee"})
    Page<PaymentSchedule> findBySchool_IdAndDeletedFalse(UUID schoolId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "fee"})
    Page<PaymentSchedule> findBySchool_IdAndStatusAndDeletedFalse(UUID schoolId, PaymentScheduleStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "fee", "enrollment", "enrollment.schoolClass"})
    @Query("SELECT ps FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.deleted = false " +
           "AND ps.status != 'PAID' AND ps.status != 'CANCELLED' AND ps.dueDate < :currentDate")
    List<PaymentSchedule> findOverdueSchedules(@Param("schoolId") UUID schoolId, @Param("currentDate") LocalDate currentDate);

    @EntityGraph(attributePaths = {"student", "fee", "enrollment", "enrollment.schoolClass"})
    @Query("SELECT ps FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.enrollment.academicYear.id = :academicYearId " +
           "AND ps.deleted = false AND ps.status != 'PAID' AND ps.status != 'CANCELLED' AND ps.dueDate < :currentDate")
    List<PaymentSchedule> findOverdueSchedulesByAcademicYear(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId,
            @Param("currentDate") LocalDate currentDate);

    @EntityGraph(attributePaths = {"student", "fee", "enrollment", "enrollment.schoolClass"})
    @Query("SELECT ps FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.enrollment.academicYear.id = :academicYearId " +
           "AND ps.enrollment.schoolClass.id = :classId AND ps.deleted = false AND ps.status != 'PAID' " +
           "AND ps.status != 'CANCELLED' AND ps.dueDate < :currentDate")
    List<PaymentSchedule> findOverdueSchedulesByAcademicYearAndClass(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId,
            @Param("classId") UUID classId,
            @Param("currentDate") LocalDate currentDate);

    @EntityGraph(attributePaths = {"student", "fee", "enrollment", "enrollment.schoolClass"})
    @Query("SELECT ps FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.enrollment.academicYear.id = :academicYearId " +
           "AND ps.deleted = false AND ps.dueDate >= :startDate AND ps.dueDate <= :endDate AND ps.status != 'CANCELLED' " +
           "ORDER BY ps.dueDate ASC")
    List<PaymentSchedule> findUpcomingSchedulesByAcademicYear(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @EntityGraph(attributePaths = {"student", "fee", "enrollment", "enrollment.schoolClass"})
    @Query("SELECT ps FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.deleted = false " +
           "AND ps.dueDate >= :startDate AND ps.dueDate <= :endDate AND ps.status != 'CANCELLED' " +
           "ORDER BY ps.dueDate ASC")
    List<PaymentSchedule> findUpcomingSchedules(
            @Param("schoolId") UUID schoolId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(ps.amountDue), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumTotalExpectedAmount(@Param("schoolId") UUID schoolId);

    @Query("SELECT COALESCE(SUM(ps.amountPaid), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumTotalCollectedAmount(@Param("schoolId") UUID schoolId);

    @Query("SELECT COALESCE(SUM(ps.remainingAmount), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumTotalOutstandingAmount(@Param("schoolId") UUID schoolId);

    @Query("SELECT COALESCE(SUM(ps.remainingAmount), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.deleted = false AND ps.status = 'OVERDUE'")
    BigDecimal sumTotalOverdueAmount(@Param("schoolId") UUID schoolId);

    @Query("SELECT COALESCE(SUM(ps.amountDue), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.enrollment.academicYear.id = :academicYearId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumTotalExpectedAmountByAcademicYear(@Param("schoolId") UUID schoolId, @Param("academicYearId") UUID academicYearId);

    @Query("SELECT COALESCE(SUM(ps.amountPaid), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.enrollment.academicYear.id = :academicYearId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumTotalCollectedAmountByAcademicYear(@Param("schoolId") UUID schoolId, @Param("academicYearId") UUID academicYearId);

    @Query("SELECT COALESCE(SUM(ps.remainingAmount), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.enrollment.academicYear.id = :academicYearId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumTotalOutstandingAmountByAcademicYear(@Param("schoolId") UUID schoolId, @Param("academicYearId") UUID academicYearId);

    @Query("SELECT COALESCE(SUM(ps.remainingAmount), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.enrollment.academicYear.id = :academicYearId AND ps.deleted = false AND (ps.status = 'OVERDUE' OR (ps.status != 'PAID' AND ps.dueDate < :currentDate))")
    BigDecimal sumTotalOverdueAmountByAcademicYear(@Param("schoolId") UUID schoolId, @Param("academicYearId") UUID academicYearId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT COALESCE(SUM(ps.amountDue), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.student.id = :studentId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumStudentTotalExpectedAmount(@Param("schoolId") UUID schoolId, @Param("studentId") UUID studentId);

    @Query("SELECT COALESCE(SUM(ps.amountPaid), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.student.id = :studentId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumStudentTotalCollectedAmount(@Param("schoolId") UUID schoolId, @Param("studentId") UUID studentId);

    @Query("SELECT COALESCE(SUM(ps.remainingAmount), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.student.id = :studentId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumStudentTotalOutstandingAmount(@Param("schoolId") UUID schoolId, @Param("studentId") UUID studentId);

    @EntityGraph(attributePaths = {"student", "fee", "enrollment", "enrollment.schoolClass"})
    List<PaymentSchedule> findBySchool_IdAndDeletedFalse(UUID schoolId);

    @EntityGraph(attributePaths = {"student", "fee", "enrollment", "enrollment.schoolClass"})
    List<PaymentSchedule> findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(UUID schoolId, UUID academicYearId);

    @EntityGraph(attributePaths = {"student", "fee", "enrollment", "enrollment.schoolClass"})
    List<PaymentSchedule> findBySchool_IdAndEnrollment_SchoolClass_IdAndDeletedFalse(UUID schoolId, UUID schoolClassId);

    @EntityGraph(attributePaths = {"student", "fee", "enrollment", "enrollment.schoolClass"})
    List<PaymentSchedule> findBySchool_IdAndEnrollment_AcademicYear_IdAndFee_FeeTypeAndDeletedFalse(UUID schoolId, UUID academicYearId, FeeType feeType);

    @EntityGraph(attributePaths = {"student", "fee", "enrollment", "enrollment.schoolClass"})
    List<PaymentSchedule> findBySchool_IdAndFee_FeeTypeAndDeletedFalse(UUID schoolId, FeeType feeType);

    long countBySchool_IdAndStatusAndDeletedFalse(UUID schoolId, PaymentScheduleStatus status);
}
