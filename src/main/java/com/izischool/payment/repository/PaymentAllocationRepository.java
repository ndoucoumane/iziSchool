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
}
