package com.izischool.notification.service;

import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.notification.domain.ReminderRule;
import com.izischool.notification.repository.ReminderRuleRepository;
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
public class ReminderService {

    private final ReminderRuleRepository reminderRuleRepository;
    private final TenantValidationService tenantValidationService;

    @Transactional
    public ReminderRule createReminderRule(ReminderRule rule) {
        tenantValidationService.validateEntityAccess(rule);
        return reminderRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<ReminderRule> getActiveReminderRules(UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return reminderRuleRepository.findBySchool_IdAndActiveTrue(schoolId);
    }

    @Transactional(readOnly = true)
    public ReminderRule getReminderRuleById(UUID id, UUID schoolId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        return reminderRuleRepository.findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("ReminderRule", id));
    }
}
