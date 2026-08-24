package com.izischool.payment.repository;

import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @EntityGraph(attributePaths = {"student", "parent"})
    Optional<Payment> findByIdAndSchool_IdAndDeletedFalse(UUID id, UUID schoolId);

    @EntityGraph(attributePaths = {"student", "parent"})
    Optional<Payment> findByPaymentReference(String paymentReference);

    @EntityGraph(attributePaths = {"student", "parent"})
    Optional<Payment> findByProviderAndProviderTransactionId(PaymentProvider provider, String providerTransactionId);

    boolean existsByProviderAndProviderTransactionId(PaymentProvider provider, String providerTransactionId);

    @EntityGraph(attributePaths = {"student", "parent"})
    Page<Payment> findBySchool_IdAndDeletedFalse(UUID schoolId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "parent"})
    Page<Payment> findBySchool_IdAndStatusAndDeletedFalse(UUID schoolId, PaymentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "parent"})
    Page<Payment> findBySchool_IdAndStudent_IdAndDeletedFalse(UUID schoolId, UUID studentId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "parent"})
    List<Payment> findBySchool_IdAndStudent_IdAndDeletedFalseOrderByPaymentDateDesc(UUID schoolId, UUID studentId);

    @EntityGraph(attributePaths = {"student", "parent"})
    Optional<Payment> findFirstBySchool_IdAndStudent_IdAndStatusAndDeletedFalseOrderByPaymentDateDesc(UUID schoolId, UUID studentId, PaymentStatus status);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.school.id = :schoolId AND p.status = 'SUCCESS' AND p.deleted = false " +
           "AND p.paymentDate >= :startTime AND p.paymentDate <= :endTime")
    long countSuccessfulPaymentsBetween(@Param("schoolId") UUID schoolId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.school.id = :schoolId AND p.status = 'SUCCESS' AND p.deleted = false " +
           "AND p.paymentDate >= :startTime AND p.paymentDate <= :endTime")
    BigDecimal sumSuccessfulPaymentsBetween(@Param("schoolId") UUID schoolId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @EntityGraph(attributePaths = {"student", "parent"})
    List<Payment> findBySchool_IdAndDeletedFalse(UUID schoolId);

    @EntityGraph(attributePaths = {"student", "parent"})
    List<Payment> findBySchool_IdAndStatusAndDeletedFalse(UUID schoolId, PaymentStatus status);

    @Query("SELECT p.paymentMethod, COUNT(p), COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.school.id = :schoolId AND p.status = 'SUCCESS' AND p.deleted = false GROUP BY p.paymentMethod")
    List<Object[]> sumAmountByPaymentMethod(@Param("schoolId") UUID schoolId);

    @Query("SELECT p.paymentMethod, COUNT(p), COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.school.id = :schoolId AND p.status = 'SUCCESS' AND p.deleted = false AND p.paymentDate >= :start AND p.paymentDate <= :end GROUP BY p.paymentMethod")
    List<Object[]> sumAmountByPaymentMethodBetween(@Param("schoolId") UUID schoolId, @Param("start") Instant start, @Param("end") Instant end);

    @EntityGraph(attributePaths = {"student", "parent"})
    @Query("SELECT p FROM Payment p WHERE p.school.id = :schoolId AND p.status = 'SUCCESS' AND p.deleted = false AND p.paymentDate >= :start AND p.paymentDate <= :end ORDER BY p.paymentDate ASC")
    List<Payment> findSuccessfulPaymentsBetween(@Param("schoolId") UUID schoolId, @Param("start") Instant start, @Param("end") Instant end);
}
