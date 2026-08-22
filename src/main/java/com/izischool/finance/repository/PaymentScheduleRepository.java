package com.izischool.finance.repository;

import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Optional<PaymentSchedule> findByIdAndSchool_IdAndDeletedFalse(UUID id, UUID schoolId);

    List<PaymentSchedule> findBySchool_IdAndStudent_IdAndDeletedFalseOrderByDueDateAsc(UUID schoolId, UUID studentId);

    List<PaymentSchedule> findBySchool_IdAndEnrollment_IdAndDeletedFalseOrderByDueDateAsc(UUID schoolId, UUID enrollmentId);

    List<PaymentSchedule> findBySchool_IdAndStudent_IdAndStatusInAndDeletedFalseOrderByDueDateAsc(
            UUID schoolId, UUID studentId, List<PaymentScheduleStatus> statuses);

    Page<PaymentSchedule> findBySchool_IdAndDeletedFalse(UUID schoolId, Pageable pageable);

    Page<PaymentSchedule> findBySchool_IdAndStatusAndDeletedFalse(UUID schoolId, PaymentScheduleStatus status, Pageable pageable);

    @Query("SELECT ps FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.deleted = false " +
           "AND ps.status != 'PAID' AND ps.status != 'CANCELLED' AND ps.dueDate < :currentDate")
    List<PaymentSchedule> findOverdueSchedules(@Param("schoolId") UUID schoolId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT COALESCE(SUM(ps.amountDue), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumTotalExpectedAmount(@Param("schoolId") UUID schoolId);

    @Query("SELECT COALESCE(SUM(ps.amountPaid), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumTotalCollectedAmount(@Param("schoolId") UUID schoolId);

    @Query("SELECT COALESCE(SUM(ps.remainingAmount), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.deleted = false AND ps.status != 'CANCELLED'")
    BigDecimal sumTotalOutstandingAmount(@Param("schoolId") UUID schoolId);

    @Query("SELECT COALESCE(SUM(ps.remainingAmount), 0) FROM PaymentSchedule ps WHERE ps.school.id = :schoolId AND ps.deleted = false AND ps.status = 'OVERDUE'")
    BigDecimal sumTotalOverdueAmount(@Param("schoolId") UUID schoolId);

    long countBySchool_IdAndStatusAndDeletedFalse(UUID schoolId, PaymentScheduleStatus status);
}
