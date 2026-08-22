package com.izischool.payment.repository;

import com.izischool.payment.domain.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    Optional<Receipt> findByIdAndSchool_IdAndDeletedFalse(UUID id, UUID schoolId);

    Optional<Receipt> findByPayment_Id(UUID paymentId);

    Optional<Receipt> findBySchool_IdAndReceiptNumber(UUID schoolId, String receiptNumber);

    Page<Receipt> findBySchool_IdAndDeletedFalse(UUID schoolId, Pageable pageable);

    long countBySchool_Id(UUID schoolId);
}
