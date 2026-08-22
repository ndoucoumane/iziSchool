package com.izischool.tenant.service;

import com.izischool.common.exception.TenantAccessException;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentStatus;
import com.izischool.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantValidationServiceTest {

    private TenantValidationService tenantValidationService;
    private School schoolA;
    private School schoolB;

    @BeforeEach
    void setUp() {
        tenantValidationService = new TenantValidationService();

        schoolA = School.builder()
                .id(UUID.randomUUID())
                .name("École Sainte Marie (School A)")
                .code("STE-MARIE")
                .currency("XOF")
                .status(SchoolStatus.ACTIVE)
                .build();

        schoolB = School.builder()
                .id(UUID.randomUUID())
                .name("Lycée Moderne (School B)")
                .code("LYCEE-MODERNE")
                .currency("XOF")
                .status(SchoolStatus.ACTIVE)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Cas 5 : Utilisateur de School A essaye d'accéder à School B -> TenantAccessException levée")
    void testCrossTenantAccessDenied() {
        // Authenticated user belongs to School A
        TenantContext.set(TenantContext.builder()
                .schoolId(schoolA.getId())
                .schoolCode(schoolA.getCode())
                .keycloakUserId("user-keycloak-123")
                .email("director@schoolA.com")
                .roles(Set.of("ROLE_DIRECTOR"))
                .superAdmin(false)
                .build());

        // Target student belongs to School B
        Student studentSchoolB = Student.builder()
                .id(UUID.randomUUID())
                .school(schoolB)
                .studentNumber("SCHB-STD-00042")
                .firstName("Fatou")
                .lastName("Ndiaye")
                .gender(Gender.FEMALE)
                .status(StudentStatus.ACTIVE)
                .build();

        assertThatThrownBy(() -> tenantValidationService.validateEntityAccess(studentSchoolB))
                .isInstanceOf(TenantAccessException.class)
                .hasMessageContaining("Access denied: You do not have access to school");
    }

    @Test
    @DisplayName("Accès autorisé lorsque l'utilisateur appartient au même tenant (School A -> School A)")
    void testSameTenantAccessGranted() {
        TenantContext.set(TenantContext.builder()
                .schoolId(schoolA.getId())
                .schoolCode(schoolA.getCode())
                .keycloakUserId("user-keycloak-123")
                .email("director@schoolA.com")
                .roles(Set.of("ROLE_DIRECTOR"))
                .superAdmin(false)
                .build());

        Student studentSchoolA = Student.builder()
                .id(UUID.randomUUID())
                .school(schoolA)
                .studentNumber("SCHA-STD-00001")
                .firstName("Moussa")
                .lastName("Sarr")
                .gender(Gender.MALE)
                .status(StudentStatus.ACTIVE)
                .build();

        assertThatCode(() -> tenantValidationService.validateEntityAccess(studentSchoolA))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Super Admin SaaS peut accéder à n'importe quelle école")
    void testSuperAdminGlobalAccess() {
        TenantContext.set(TenantContext.builder()
                .schoolId(null)
                .keycloakUserId("superadmin-id")
                .email("admin@izischool.com")
                .roles(Set.of("ROLE_SUPER_ADMIN"))
                .superAdmin(true)
                .build());

        assertThatCode(() -> tenantValidationService.validateSchoolAccess(schoolB.getId()))
                .doesNotThrowAnyException();
    }
}
