package com.izischool.superadmin.service;

import com.izischool.academic.repository.AcademicYearRepository;
import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserRole;
import com.izischool.auth.domain.UserStatus;
import com.izischool.auth.dto.CreateUserRequest;
import com.izischool.auth.dto.UserProfileResponse;
import com.izischool.auth.repository.UserProfileRepository;
import com.izischool.auth.service.KeycloakAdminService;
import com.izischool.auth.service.UserService;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentStatus;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.school.dto.SchoolResponse;
import com.izischool.school.repository.SchoolRepository;
import com.izischool.school.service.SchoolService;
import com.izischool.student.repository.StudentRepository;
import com.izischool.superadmin.dto.CreateSchoolWithAdminRequest;
import com.izischool.superadmin.dto.SuperAdminStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final SchoolRepository schoolRepository;
    private final SchoolService schoolService;
    private final AcademicYearRepository academicYearRepository;
    private final StudentRepository studentRepository;
    private final PaymentRepository paymentRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserService userService;
    private final KeycloakAdminService keycloakAdminService;

    /**
     * Calcule et retourne les métriques et statistiques globales de la plateforme SaaS.
     */
    @Transactional(readOnly = true)
    public SuperAdminStatsResponse getPlatformStats() {
        List<School> schools = schoolRepository.findAll();
        long activeSchools = schools.stream().filter(s -> s.getStatus() == SchoolStatus.ACTIVE).count();
        long suspendedSchools = schools.stream().filter(s -> s.getStatus() == SchoolStatus.SUSPENDED).count();
        long inactiveSchools = schools.stream().filter(s -> s.getStatus() == SchoolStatus.INACTIVE).count();

        long totalStudents = studentRepository.count();

        List<Payment> payments = paymentRepository.findAll();
        BigDecimal totalRevenue = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS && !p.isDeleted())
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long successfulPaymentsCount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS && !p.isDeleted())
                .count();

        long totalUsers = userProfileRepository.count();

        return SuperAdminStatsResponse.builder()
                .totalSchools(schools.size())
                .activeSchools(activeSchools)
                .suspendedSchools(suspendedSchools)
                .inactiveSchools(inactiveSchools)
                .totalStudents(totalStudents)
                .totalCollectedRevenue(totalRevenue)
                .totalSuccessfulPayments(successfulPaymentsCount)
                .totalUsers(totalUsers)
                .build();
    }

    /**
     * Crée un nouvel établissement et optionnellement son compte Directeur/Admin initial.
     */
    @Transactional
    public SchoolResponse createSchoolWithAdmin(CreateSchoolWithAdminRequest request) {
        School school = request.toSchoolEntity();
        School createdSchool = schoolService.createSchool(school);

        // Initialisation automatique de l'année scolaire active par défaut
        int currentYear = java.time.LocalDate.now().getYear();
        String academicYearName = String.format("%d-%d", currentYear, currentYear + 1);
        if (!academicYearRepository.existsBySchool_IdAndName(createdSchool.getId(), academicYearName)) {
            com.izischool.academic.domain.AcademicYear defaultYear = com.izischool.academic.domain.AcademicYear.builder()
                    .school(createdSchool)
                    .name(academicYearName)
                    .startDate(java.time.LocalDate.of(currentYear, 9, 1))
                    .endDate(java.time.LocalDate.of(currentYear + 1, 6, 30))
                    .status(com.izischool.academic.domain.AcademicYearStatus.ACTIVE)
                    .build();
            academicYearRepository.save(defaultYear);
            log.info("Année scolaire active [{}] initialisée automatiquement pour l'école {}", academicYearName, createdSchool.getCode());
        }

        if (request.getAdminEmail() != null && !request.getAdminEmail().isBlank()) {
            String password = (request.getAdminPassword() != null && !request.getAdminPassword().isBlank())
                    ? request.getAdminPassword()
                    : "Passer1234!";

            String firstName = request.getAdminFirstName() != null ? request.getAdminFirstName() : "Directeur";
            String lastName = request.getAdminLastName() != null ? request.getAdminLastName() : "Principal";

            String keycloakId = keycloakAdminService.createKeycloakUser(
                    request.getAdminEmail(),
                    password,
                    firstName,
                    lastName,
                    UserRole.DIRECTOR
            );

            UserProfile adminProfile = UserProfile.builder()
                    .keycloakUserId(keycloakId)
                    .school(createdSchool)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(request.getAdminEmail())
                    .phone(request.getAdminPhone())
                    .role(UserRole.DIRECTOR)
                    .status(UserStatus.ACTIVE)
                    .build();

            userService.createUserProfile(adminProfile);
            log.info("Compte Directeur/Administrateur actif créé pour l'école {}: {}", createdSchool.getCode(), request.getAdminEmail());
        }

        return SchoolResponse.fromEntity(createdSchool);
    }

    /**
     * Retourne la liste des utilisateurs administratifs d'un établissement.
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getSchoolAdmins(UUID schoolId) {
        schoolService.getSchoolById(schoolId);
        List<UserProfile> admins = userProfileRepository.findBySchool_IdAndRoleIn(
                schoolId,
                List.of(UserRole.DIRECTOR, UserRole.ADMIN, UserRole.ACCOUNTANT)
        );
        return admins.stream().map(UserProfileResponse::fromEntity).collect(Collectors.toList());
    }

    /**
     * Crée un compte administratif pour un établissement existant.
     */
    @Transactional
    public UserProfileResponse createSchoolAdmin(UUID schoolId, CreateUserRequest request) {
        School school = schoolService.getSchoolById(schoolId);
        String keycloakId = (request.getKeycloakUserId() != null && !request.getKeycloakUserId().isBlank())
                ? request.getKeycloakUserId()
                : keycloakAdminService.createKeycloakUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getRole()
        );

        UserProfile userProfile = request.toEntity(school);
        userProfile.setKeycloakUserId(keycloakId);

        UserProfile created = userService.createUserProfile(userProfile);
        return UserProfileResponse.fromEntity(created);
    }

    /**
     * Réinitialise le mot de passe d'un utilisateur dans Keycloak.
     */
    @Transactional
    public void resetUserPassword(UUID userId, String newPassword) {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", userId));

        keycloakAdminService.updateUserPassword(user.getKeycloakUserId(), newPassword);
        log.info("Mot de passe réinitialisé par SUPER_ADMIN pour l'utilisateur: {}", user.getEmail());
    }

    /**
     * Active, suspend ou désactive un compte utilisateur.
     */
    @Transactional
    public UserProfileResponse updateUserStatus(UUID userId, UserStatus status) {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", userId));

        user.setStatus(status);
        UserProfile updated = userProfileRepository.save(user);
        log.info("Statut mis à jour pour l'utilisateur {} vers {}", user.getEmail(), status);
        return UserProfileResponse.fromEntity(updated);
    }
}
