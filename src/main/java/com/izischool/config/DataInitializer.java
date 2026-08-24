package com.izischool.config;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.AcademicYearStatus;
import com.izischool.academic.domain.GradeLevel;
import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.domain.SchoolClassStatus;
import com.izischool.academic.repository.AcademicYearRepository;
import com.izischool.academic.repository.GradeLevelRepository;
import com.izischool.academic.repository.SchoolClassRepository;
import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserRole;
import com.izischool.auth.domain.UserStatus;
import com.izischool.auth.repository.UserProfileRepository;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final UserProfileRepository userProfileRepository;
    private final AcademicYearRepository academicYearRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Value("${izischool.dev-seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${izischool.dev-seed.school-code:PILOTE-01}")
    private String defaultSchoolCode;

    @Value("${izischool.dev-seed.school-name:École Pilote iziSchool}")
    private String defaultSchoolName;

    @Value("${izischool.dev-seed.admin-email:admin@izischool.com}")
    private String defaultAdminEmail;

    @Value("${izischool.dev-seed.admin-first-name:Admin}")
    private String defaultAdminFirstName;

    @Value("${izischool.dev-seed.admin-last-name:Principal}")
    private String defaultAdminLastName;

    @Value("${izischool.dev-seed.keycloak-user-id:dev-default-admin-id}")
    private String defaultKeycloakUserId;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        // 1. Initialize Default School Tenant
        School school = schoolRepository.findByCode(defaultSchoolCode)
                .orElseGet(() -> {
                    School newSchool = School.builder()
                            .name(defaultSchoolName)
                            .code(defaultSchoolCode)
                            .email(defaultAdminEmail)
                            .phone("+221770000000")
                            .address("Dakar, Sénégal")
                            .city("Dakar")
                            .country("Sénégal")
                            .currency("XOF")
                            .status(SchoolStatus.ACTIVE)
                            .build();
                    return schoolRepository.save(newSchool);
                });

        // 2. Initialize Default Admin User Profile (SUPER_ADMIN / DIRECTOR)
        Optional<UserProfile> existingAdmin = userProfileRepository.findByKeycloakUserId(defaultKeycloakUserId);
        if (existingAdmin.isEmpty()) {
            UserProfile adminProfile = UserProfile.builder()
                    .keycloakUserId(defaultKeycloakUserId)
                    .school(school)
                    .firstName(defaultAdminFirstName)
                    .lastName(defaultAdminLastName)
                    .email(defaultAdminEmail)
                    .phone("+221770000000")
                    .role(UserRole.SUPER_ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            userProfileRepository.save(adminProfile);
        }

        // 3. Initialize Default Academic Year
        if (!academicYearRepository.existsBySchool_IdAndName(school.getId(), "2026-2027")) {
            AcademicYear year = AcademicYear.builder()
                    .school(school)
                    .name("2026-2027")
                    .startDate(LocalDate.of(2026, 10, 1))
                    .endDate(LocalDate.of(2027, 6, 30))
                    .status(AcademicYearStatus.ACTIVE)
                    .build();
            academicYearRepository.save(year);

            // 4. Initialize Default Grade Level & Class
            GradeLevel grade = GradeLevel.builder()
                    .school(school)
                    .name("6ème")
                    .code("6EME")
                    .displayOrder(1)
                    .active(true)
                    .build();
            gradeLevelRepository.save(grade);

            SchoolClass schoolClass = SchoolClass.builder()
                    .school(school)
                    .academicYear(year)
                    .gradeLevel(grade)
                    .name("6ème A")
                    .code("6A")
                    .capacity(35)
                    .status(SchoolClassStatus.ACTIVE)
                    .build();
            schoolClassRepository.save(schoolClass);
        }

        // Clean & Elegant Console Startup Banner
        log.info("==================================================================================");
        log.info("  🚀 iziSchool Backend — Démarré avec succès sur le port 8081");
        log.info("==================================================================================");
        log.info("  ⭐ Mode Actif     : Développement (Accès libre / JWT bypass)");
        log.info("  🏫 École Pilote   : {} [{}]", school.getName(), school.getCode());
        log.info("  👤 Admin Profil   : {} (Rôle: SUPER_ADMIN)", defaultAdminEmail);
        log.info("  📚 Documentation  : http://localhost:8081/api/v1/swagger-ui.html");
        log.info("  🩺 Health Check   : http://localhost:8081/api/v1/actuator/health");
        log.info("==================================================================================");
    }
}
