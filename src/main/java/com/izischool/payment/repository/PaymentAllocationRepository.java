package com.izischool.payment.repository;

import com.izischool.payment.domain.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, UUID> {

    List<PaymentAllocation> findByPayment_Id(UUID paymentId);

    List<PaymentAllocation> findByPaymentSchedule_Id(UUID paymentScheduleId);

    @Query("SELECT COALESCE(SUM(pa.amount), 0) FROM PaymentAllocation pa WHERE pa.paymentSchedule.id = :scheduleId")
    BigDecimal sumAllocatedAmountByScheduleId(@Param("scheduleId") UUID scheduleId);

    @Query("SELECT pa FROM PaymentAllocation pa " +
           "JOIN FETCH pa.paymentSchedule ps " +
           "JOIN FETCH ps.fee f " +
           "JOIN FETCH pa.payment p " +
           "WHERE ps.school.id = :schoolId AND p.status = 'SUCCESS' AND p.deleted = false")
    List<PaymentAllocation> findAllSuccessfulAllocationsWithFeeBySchoolId(@Param("schoolId") UUID schoolId);
}
