package com.izischool.finance.service;

import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.finance.domain.PaymentPlan;
import com.izischool.finance.repository.PaymentPlanRepository;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPlanService {

    private final PaymentPlanRepository paymentPlanRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public PaymentPlan createPaymentPlan(PaymentPlan plan) {
        tenantValidationService.validateEntityAccess(plan);
        return paymentPlanRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public PaymentPlan getPaymentPlanById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return paymentPlanRepository.findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentPlan", id));
    }

    @Transactional(readOnly = true)
    public List<PaymentPlan> getActivePlansByAcademicYear(UUID schoolId, UUID academicYearId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return paymentPlanRepository.findBySchool_IdAndAcademicYear_IdAndActiveTrue(schoolId, academicYearId);
    }
}
