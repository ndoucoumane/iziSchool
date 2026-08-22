package com.izischool.payment.repository;

import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndSchool_IdAndDeletedFalse(UUID id, UUID schoolId);

    Optional<Payment> findByPaymentReference(String paymentReference);

    Optional<Payment> findByProviderAndProviderTransactionId(PaymentProvider provider, String providerTransactionId);

    boolean existsByProviderAndProviderTransactionId(PaymentProvider provider, String providerTransactionId);

    Page<Payment> findBySchool_IdAndDeletedFalse(UUID schoolId, Pageable pageable);

    Page<Payment> findBySchool_IdAndStatusAndDeletedFalse(UUID schoolId, PaymentStatus status, Pageable pageable);

    Page<Payment> findBySchool_IdAndStudent_IdAndDeletedFalse(UUID schoolId, UUID studentId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.school.id = :schoolId AND p.status = 'SUCCESS' AND p.deleted = false " +
           "AND p.paymentDate >= :startTime AND p.paymentDate <= :endTime")
    long countSuccessfulPaymentsBetween(@Param("schoolId") UUID schoolId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.school.id = :schoolId AND p.status = 'SUCCESS' AND p.deleted = false " +
           "AND p.paymentDate >= :startTime AND p.paymentDate <= :endTime")
    BigDecimal sumSuccessfulPaymentsBetween(@Param("schoolId") UUID schoolId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}
