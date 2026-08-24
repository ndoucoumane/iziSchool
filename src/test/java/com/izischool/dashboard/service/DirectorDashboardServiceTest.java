package com.izischool.dashboard.service;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.AcademicYearStatus;
import com.izischool.academic.domain.GradeLevel;
import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.domain.SchoolClassStatus;
import com.izischool.academic.repository.AcademicYearRepository;
import com.izischool.academic.repository.SchoolClassRepository;
import com.izischool.common.response.PageResponse;
import com.izischool.dashboard.dto.ClassEnrollmentStatsResponse;
import com.izischool.dashboard.dto.DirectorDashboardResponse;
import com.izischool.dashboard.dto.DirectorPaymentSummaryResponse;
import com.izischool.dashboard.dto.FeeCategoryBreakdownResponse;
import com.izischool.dashboard.dto.FeeTypeSummaryResponse;
import com.izischool.dashboard.dto.GlobalEnrollmentStatsResponse;
import com.izischool.dashboard.dto.MonthlyScheduleStatusResponse;
import com.izischool.dashboard.dto.OverdueStudentPaymentResponse;
import com.izischool.dashboard.dto.PaymentEvolutionResponse;
import com.izischool.dashboard.dto.PaymentMethodBreakdownResponse;
import com.izischool.dashboard.dto.StudentFinancialStatusResponse;
import com.izischool.dashboard.dto.UpcomingDueDateResponse;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeType;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.repository.FeeRepository;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.parent.domain.Parent;
import com.izischool.parent.domain.ParentRelationship;
import com.izischool.parent.domain.StudentParent;
import com.izischool.parent.repository.StudentParentRepository;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.school.service.SchoolService;
import com.izischool.student.domain.EnrollmentStatus;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentEnrollment;
import com.izischool.student.domain.StudentStatus;
import com.izischool.student.repository.StudentEnrollmentRepository;
import com.izischool.student.repository.StudentRepository;
import com.izischool.tenant.service.TenantValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectorDashboardServiceTest {

    @Mock
    private SchoolService schoolService;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock
    private PaymentScheduleRepository paymentScheduleRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private FeeRepository feeRepository;
    @Mock
    private StudentParentRepository studentParentRepository;
    @Mock
    private TenantValidationService tenantValidationService;

    @InjectMocks
    private DirectorDashboardService directorDashboardService;

    private School school;
    private AcademicYear academicYear;
    private GradeLevel gradeLevel;
    private SchoolClass schoolClass;
    private Student student1;
    private Student student2;
    private StudentEnrollment enrollment1;
    private StudentEnrollment enrollment2;
    private Fee feeRegistration;
    private Fee feeTuition;
    private PaymentSchedule schedule1;
    private PaymentSchedule schedule2;

    @BeforeEach
    void setUp() {
        school = School.builder()
                .id(UUID.randomUUID())
                .name("Groupe Scolaire Excellence")
                .code("GSE-DAK")
                .currency("XOF")
                .status(SchoolStatus.ACTIVE)
                .build();

        academicYear = AcademicYear.builder()
                .id(UUID.randomUUID())
                .school(school)
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2027, 6, 30))
                .status(AcademicYearStatus.ACTIVE)
                .build();

        gradeLevel = GradeLevel.builder()
                .id(UUID.randomUUID())
                .school(school)
                .name("6ème")
                .code("6EME")
                .displayOrder(1)
                .build();

        schoolClass = SchoolClass.builder()
                .id(UUID.randomUUID())
                .school(school)
                .academicYear(academicYear)
                .gradeLevel(gradeLevel)
                .name("6ème A")
                .code("6A")
                .capacity(30)
                .status(SchoolClassStatus.ACTIVE)
                .build();

        student1 = Student.builder()
                .id(UUID.randomUUID())
                .school(school)
                .studentNumber("STD-001")
                .firstName("Amadou")
                .lastName("Ba")
                .gender(Gender.MALE)
                .status(StudentStatus.ACTIVE)
                .build();

        student2 = Student.builder()
                .id(UUID.randomUUID())
                .school(school)
                .studentNumber("STD-002")
                .firstName("Fatou")
                .lastName("Sow")
                .gender(Gender.FEMALE)
                .status(StudentStatus.ACTIVE)
                .build();

        enrollment1 = StudentEnrollment.builder()
                .id(UUID.randomUUID())
                .school(school)
                .student(student1)
                .academicYear(academicYear)
                .schoolClass(schoolClass)
                .enrollmentDate(LocalDate.of(2026, 9, 2))
                .status(EnrollmentStatus.ACTIVE)
                .build();

        enrollment2 = StudentEnrollment.builder()
                .id(UUID.randomUUID())
                .school(school)
                .student(student2)
                .academicYear(academicYear)
                .schoolClass(schoolClass)
                .enrollmentDate(LocalDate.of(2026, 9, 3))
                .status(EnrollmentStatus.PENDING)
                .build();

        feeRegistration = Fee.builder()
                .id(UUID.randomUUID())
                .school(school)
                .academicYear(academicYear)
                .name("Frais d'inscription")
                .code("INSCR-2026")
                .feeType(FeeType.REGISTRATION)
                .amount(new BigDecimal("50000.00"))
                .currency("XOF")
                .build();

        feeTuition = Fee.builder()
                .id(UUID.randomUUID())
                .school(school)
                .academicYear(academicYear)
                .name("Mensualité Octobre")
                .code("MENS-OCT")
                .feeType(FeeType.TUITION)
                .amount(new BigDecimal("30000.00"))
                .currency("XOF")
                .build();

        schedule1 = PaymentSchedule.builder()
                .id(UUID.randomUUID())
                .school(school)
                .student(student1)
                .enrollment(enrollment1)
                .fee(feeRegistration)
                .dueDate(LocalDate.of(2026, 9, 15))
                .amountDue(new BigDecimal("50000.00"))
                .amountPaid(new BigDecimal("50000.00"))
                .remainingAmount(BigDecimal.ZERO)
                .status(PaymentScheduleStatus.PAID)
                .currency("XOF")
                .installmentNumber(1)
                .build();

        schedule2 = PaymentSchedule.builder()
                .id(UUID.randomUUID())
                .school(school)
                .student(student2)
                .enrollment(enrollment2)
                .fee(feeTuition)
                .dueDate(LocalDate.now().minusDays(10))
                .amountDue(new BigDecimal("30000.00"))
                .amountPaid(new BigDecimal("10000.00"))
                .remainingAmount(new BigDecimal("20000.00"))
                .status(PaymentScheduleStatus.OVERDUE)
                .currency("XOF")
                .installmentNumber(2)
                .build();
    }

    @Test
    @DisplayName("1. Statistiques globales des effectifs avec données")
    void testGetGlobalEnrollmentStats_withData() {
        UUID schoolId = school.getId();
        when(academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.of(academicYear));
        when(studentRepository.countBySchool_IdAndDeletedFalse(schoolId)).thenReturn(2L);
        when(schoolClassRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYear.getId()))
                .thenReturn(List.of(schoolClass));
        when(studentEnrollmentRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYear.getId()))
                .thenReturn(List.of(enrollment1, enrollment2));

        GlobalEnrollmentStatsResponse stats = directorDashboardService.getGlobalEnrollmentStats(schoolId, null);

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalStudents()).isEqualTo(2L);
        assertThat(stats.getTotalClasses()).isEqualTo(1L);
        assertThat(stats.getTotalCapacity()).isEqualTo(30L);
        assertThat(stats.getTotalEnrolledStudents()).isEqualTo(2L);
        assertThat(stats.getTotalAvailableSeats()).isEqualTo(28L);
        assertThat(stats.getGlobalEnrollmentRate()).isEqualTo(6.67);
        assertThat(stats.getActiveRegistrations()).isEqualTo(1L);
        assertThat(stats.getPendingRegistrations()).isEqualTo(1L);
    }

    @Test
    @DisplayName("2. Statistiques globales avec école vide (0 élève, 0 classe)")
    void testGetGlobalEnrollmentStats_emptySchool() {
        UUID schoolId = school.getId();
        when(academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(academicYearRepository.findBySchool_IdOrderByStartDateDesc(schoolId))
                .thenReturn(List.of());
        when(studentRepository.countBySchool_IdAndDeletedFalse(schoolId)).thenReturn(0L);

        GlobalEnrollmentStatsResponse stats = directorDashboardService.getGlobalEnrollmentStats(schoolId, null);

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalStudents()).isEqualTo(0L);
        assertThat(stats.getTotalClasses()).isEqualTo(0L);
        assertThat(stats.getTotalCapacity()).isEqualTo(0L);
        assertThat(stats.getTotalAvailableSeats()).isEqualTo(0L);
        assertThat(stats.getGlobalEnrollmentRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("3. Situation des classes (effectifs, taux d'occupation, finances)")
    void testGetClassesEnrollmentStats() {
        UUID schoolId = school.getId();
        when(academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.of(academicYear));
        when(schoolService.getSchoolById(schoolId)).thenReturn(school);
        when(schoolClassRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYear.getId()))
                .thenReturn(List.of(schoolClass));
        when(studentEnrollmentRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYear.getId()))
                .thenReturn(List.of(enrollment1, enrollment2));
        when(paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(schoolId, academicYear.getId()))
                .thenReturn(List.of(schedule1, schedule2));

        List<ClassEnrollmentStatsResponse> list = directorDashboardService.getClassesEnrollmentStats(schoolId, null);

        assertThat(list).hasSize(1);
        ClassEnrollmentStatsResponse cStats = list.get(0);
        assertThat(cStats.getClassName()).isEqualTo("6ème A");
        assertThat(cStats.getCapacity()).isEqualTo(30);
        assertThat(cStats.getEnrolledCount()).isEqualTo(2);
        assertThat(cStats.getAvailableSeats()).isEqualTo(28);
        assertThat(cStats.getOccupancyRate()).isEqualTo(6.67);
        assertThat(cStats.getTotalExpected()).isEqualByComparingTo("80000.00");
        assertThat(cStats.getTotalCollected()).isEqualByComparingTo("60000.00");
        assertThat(cStats.getTotalRemaining()).isEqualByComparingTo("20000.00");
        assertThat(cStats.getCollectionRate()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("4. Synthèse financière et recouvrement globale")
    void testGetPaymentSummary() {
        UUID schoolId = school.getId();
        when(academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.of(academicYear));
        when(schoolService.getSchoolById(schoolId)).thenReturn(school);
        when(paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(schoolId, academicYear.getId()))
                .thenReturn(List.of(schedule1, schedule2));
        when(paymentRepository.sumSuccessfulPaymentsBetween(eq(schoolId), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("50000.00"));
        when(studentRepository.countBySchool_IdAndDeletedFalse(schoolId)).thenReturn(2L);

        DirectorPaymentSummaryResponse summary = directorDashboardService.getPaymentSummary(schoolId, null);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalExpected()).isEqualByComparingTo("80000.00");
        assertThat(summary.getTotalCollected()).isEqualByComparingTo("60000.00");
        assertThat(summary.getTotalRemaining()).isEqualByComparingTo("20000.00");
        assertThat(summary.getTotalOverdue()).isEqualByComparingTo("20000.00");
        assertThat(summary.getCollectionRate()).isEqualByComparingTo("75.00");
        assertThat(summary.getUpToDateStudentsCount()).isEqualTo(1L);
        assertThat(summary.getOverdueStudentsCount()).isEqualTo(1L);
        assertThat(summary.getTotalStudentsCount()).isEqualTo(2L);
        assertThat(summary.getCurrency()).isEqualTo("XOF");
    }

    @Test
    @DisplayName("5. Synthèse par type de frais (REGISTRATION)")
    void testGetFeeTypeSummary_Registrations() {
        UUID schoolId = school.getId();
        when(academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.of(academicYear));
        when(schoolService.getSchoolById(schoolId)).thenReturn(school);
        when(paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndFee_FeeTypeAndDeletedFalse(
                schoolId, academicYear.getId(), FeeType.REGISTRATION))
                .thenReturn(List.of(schedule1));

        FeeTypeSummaryResponse summary = directorDashboardService.getFeeTypeSummary(schoolId, null, FeeType.REGISTRATION);

        assertThat(summary).isNotNull();
        assertThat(summary.getFeeType()).isEqualTo(FeeType.REGISTRATION);
        assertThat(summary.getTotalExpected()).isEqualByComparingTo("50000.00");
        assertThat(summary.getTotalCollected()).isEqualByComparingTo("50000.00");
        assertThat(summary.getTotalRemaining()).isEqualByComparingTo("0.00");
        assertThat(summary.getCollectionRate()).isEqualByComparingTo("100.00");
        assertThat(summary.getFullyPaidStudentsCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("6. Suivi des mensualités / échéances du mois")
    void testGetMonthlyScheduleStatus() {
        UUID schoolId = school.getId();
        when(academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.of(academicYear));
        when(schoolService.getSchoolById(schoolId)).thenReturn(school);

        LocalDate today = LocalDate.now();
        schedule2.setDueDate(today);

        when(paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(schoolId, academicYear.getId()))
                .thenReturn(List.of(schedule1, schedule2));

        MonthlyScheduleStatusResponse response = directorDashboardService.getMonthlyScheduleStatus(
                schoolId, null, today.getMonthValue(), today.getYear());

        assertThat(response).isNotNull();
        assertThat(response.getMonth()).isEqualTo(today.getMonthValue());
        assertThat(response.getYear()).isEqualTo(today.getYear());
        assertThat(response.getTotalExpected()).isEqualByComparingTo("30000.00");
        assertThat(response.getTotalCollected()).isEqualByComparingTo("10000.00");
        assertThat(response.getTotalRemaining()).isEqualByComparingTo("20000.00");
        assertThat(response.getCurrency()).isEqualTo("XOF");
    }

    @Test
    @DisplayName("7. Situation financière individuelle des élèves (Paginée)")
    void testGetStudentsFinancialStatus() {
        UUID schoolId = school.getId();
        when(academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.of(academicYear));
        when(schoolService.getSchoolById(schoolId)).thenReturn(school);
        when(studentEnrollmentRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYear.getId()))
                .thenReturn(List.of(enrollment1, enrollment2));
        when(paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(schoolId, academicYear.getId()))
                .thenReturn(List.of(schedule1, schedule2));
        when(paymentRepository.findFirstBySchool_IdAndStudent_IdAndStatusAndDeletedFalseOrderByPaymentDateDesc(
                eq(schoolId), any(UUID.class), eq(PaymentStatus.SUCCESS)))
                .thenReturn(Optional.empty());

        PageResponse<StudentFinancialStatusResponse> page = directorDashboardService.getStudentsFinancialStatus(
                schoolId, null, null, null, null, PageRequest.of(0, 10));

        assertThat(page).isNotNull();
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);

        StudentFinancialStatusResponse s1 = page.getContent().get(0);
        assertThat(s1.getStudentNumber()).isEqualTo("STD-001");
        assertThat(s1.getStatus()).isEqualTo("UP_TO_DATE");

        StudentFinancialStatusResponse s2 = page.getContent().get(1);
        assertThat(s2.getStudentNumber()).isEqualTo("STD-002");
        assertThat(s2.getStatus()).isEqualTo("OVERDUE");
    }

    @Test
    @DisplayName("8. Répartition par catégorie de frais")
    void testGetFeeCategoryBreakdown() {
        UUID schoolId = school.getId();
        when(academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.of(academicYear));
        when(schoolService.getSchoolById(schoolId)).thenReturn(school);
        when(feeRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYear.getId()))
                .thenReturn(List.of(feeRegistration, feeTuition));
        when(paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(schoolId, academicYear.getId()))
                .thenReturn(List.of(schedule1, schedule2));

        List<FeeCategoryBreakdownResponse> list = directorDashboardService.getFeeCategoryBreakdown(schoolId, null);

        assertThat(list).hasSize(2);
        FeeCategoryBreakdownResponse r1 = list.get(0);
        assertThat(r1.getFeeCode()).isEqualTo("INSCR-2026");
        assertThat(r1.getExpectedAmount()).isEqualByComparingTo("50000.00");
        assertThat(r1.getCollectedAmount()).isEqualByComparingTo("50000.00");
        assertThat(r1.getPercentageOfTotalCollected()).isEqualByComparingTo("83.33");
    }

    @Test
    @DisplayName("9. Répartition par moyen de paiement")
    void testGetPaymentMethodBreakdown() {
        UUID schoolId = school.getId();
        when(schoolService.getSchoolById(schoolId)).thenReturn(school);

        List<Object[]> rawAggregates = List.of(
                new Object[]{PaymentMethod.MOBILE_MONEY, 15L, new BigDecimal("450000.00")},
                new Object[]{PaymentMethod.CASH, 5L, new BigDecimal("150000.00")}
        );
        when(paymentRepository.sumAmountByPaymentMethod(schoolId)).thenReturn(rawAggregates);

        List<PaymentMethodBreakdownResponse> list = directorDashboardService.getPaymentMethodBreakdown(
                schoolId, null, null, null);

        assertThat(list).isNotEmpty();
        PaymentMethodBreakdownResponse top = list.get(0);
        assertThat(top.getPaymentMethod()).isEqualTo(PaymentMethod.MOBILE_MONEY);
        assertThat(top.getTotalAmount()).isEqualByComparingTo("450000.00");
        assertThat(top.getTransactionCount()).isEqualTo(15L);
        assertThat(top.getPercentage()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("10. Évolution chronologique des encaissements")
    void testGetPaymentEvolution() {
        UUID schoolId = school.getId();
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();

        Payment p1 = Payment.builder()
                .id(UUID.randomUUID())
                .school(school)
                .paymentDate(Instant.now())
                .amount(new BigDecimal("50000.00"))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findSuccessfulPaymentsBetween(eq(schoolId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(p1));

        List<PaymentEvolutionResponse> list = directorDashboardService.getPaymentEvolution(
                schoolId, null, "DAY", start, end);

        assertThat(list).isNotEmpty();
        assertThat(list.size()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("11. Suivi des impayés et retards avec contact parent financier")
    void testGetOverdueStudents() {
        UUID schoolId = school.getId();
        when(academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.of(academicYear));
        when(schoolService.getSchoolById(schoolId)).thenReturn(school);
        when(paymentScheduleRepository.findOverdueSchedulesByAcademicYear(eq(schoolId), eq(academicYear.getId()), any(LocalDate.class)))
                .thenReturn(List.of(schedule2));

        Parent parent = Parent.builder()
                .id(UUID.randomUUID())
                .school(school)
                .firstName("Moussa")
                .lastName("Sow")
                .phone("+221776543210")
                .build();

        StudentParent sp = StudentParent.builder()
                .id(UUID.randomUUID())
                .student(student2)
                .parent(parent)
                .relationship(ParentRelationship.FATHER)
                .isFinancialContact(true)
                .build();

        when(studentParentRepository.findFinancialContactByStudentId(student2.getId()))
                .thenReturn(Optional.of(sp));

        PageResponse<OverdueStudentPaymentResponse> page = directorDashboardService.getOverdueStudents(
                schoolId, null, null, PageRequest.of(0, 10));

        assertThat(page).isNotNull();
        assertThat(page.getTotalElements()).isEqualTo(1);
        OverdueStudentPaymentResponse overdue = page.getContent().get(0);
        assertThat(overdue.getStudentName()).isEqualTo("Fatou Sow");
        assertThat(overdue.getOverdueAmount()).isEqualByComparingTo("20000.00");
        assertThat(overdue.getDaysOverdue()).isEqualTo(10L);
        assertThat(overdue.getArrearsBucket()).isEqualTo("8-30j");
        assertThat(overdue.getParentName()).isEqualTo("Moussa Sow");
        assertThat(overdue.getParentPhone()).isEqualTo("+221776543210");
    }

    @Test
    @DisplayName("12. Échéances prévisionnelles à venir")
    void testGetUpcomingDueDates() {
        UUID schoolId = school.getId();
        when(academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.of(academicYear));
        when(schoolService.getSchoolById(schoolId)).thenReturn(school);

        LocalDate futureDue = LocalDate.now().plusDays(15);
        PaymentSchedule futureSchedule = PaymentSchedule.builder()
                .id(UUID.randomUUID())
                .school(school)
                .student(student1)
                .enrollment(enrollment1)
                .fee(feeTuition)
                .dueDate(futureDue)
                .amountDue(new BigDecimal("30000.00"))
                .amountPaid(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("30000.00"))
                .status(PaymentScheduleStatus.PENDING)
                .currency("XOF")
                .build();

        when(paymentScheduleRepository.findUpcomingSchedulesByAcademicYear(eq(schoolId), eq(academicYear.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(futureSchedule));

        List<UpcomingDueDateResponse> list = directorDashboardService.getUpcomingDueDates(schoolId, null, 30);

        assertThat(list).hasSize(1);
        UpcomingDueDateResponse item = list.get(0);
        assertThat(item.getDueDate()).isEqualTo(futureDue);
        assertThat(item.getFeeName()).isEqualTo("Mensualité Octobre");
        assertThat(item.getTotalAmountExpected()).isEqualByComparingTo("30000.00");
        assertThat(item.getPendingStudentsCount()).isEqualTo(1L);
        assertThat(item.getDaysRemaining()).isEqualTo(15L);
        assertThat(item.isToday()).isFalse();
    }

    @Test
    @DisplayName("13. Vue d'ensemble agrégée complète tout-en-un")
    void testGetDirectorDashboard() {
        UUID schoolId = school.getId();
        when(academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.of(academicYear));
        when(academicYearRepository.findByIdAndSchool_Id(academicYear.getId(), schoolId))
                .thenReturn(Optional.of(academicYear));
        when(schoolService.getSchoolById(schoolId)).thenReturn(school);
        when(studentRepository.countBySchool_IdAndDeletedFalse(schoolId)).thenReturn(2L);
        when(schoolClassRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYear.getId()))
                .thenReturn(List.of(schoolClass));
        when(studentEnrollmentRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYear.getId()))
                .thenReturn(List.of(enrollment1, enrollment2));
        when(paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(schoolId, academicYear.getId()))
                .thenReturn(List.of(schedule1, schedule2));
        when(paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndFee_FeeTypeAndDeletedFalse(
                eq(schoolId), eq(academicYear.getId()), any(FeeType.class)))
                .thenReturn(List.of(schedule1));
        when(feeRepository.findBySchool_IdAndAcademicYear_Id(schoolId, academicYear.getId()))
                .thenReturn(List.of(feeRegistration));
        when(paymentRepository.sumAmountByPaymentMethod(schoolId)).thenReturn(List.of());
        when(paymentRepository.findSuccessfulPaymentsBetween(eq(schoolId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(paymentScheduleRepository.findOverdueSchedulesByAcademicYear(eq(schoolId), eq(academicYear.getId()), any(LocalDate.class)))
                .thenReturn(List.of());
        when(paymentScheduleRepository.findUpcomingSchedulesByAcademicYear(eq(schoolId), eq(academicYear.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        DirectorDashboardResponse response = directorDashboardService.getDirectorDashboard(schoolId, null);

        assertThat(response).isNotNull();
        assertThat(response.getAcademicYearName()).isEqualTo("2026-2027");
        assertThat(response.getEnrollment()).isNotNull();
        assertThat(response.getPayments()).isNotNull();
        assertThat(response.getRegistrations()).isNotNull();
        assertThat(response.getTuition()).isNotNull();
        assertThat(response.getCurrentMonthSchedules()).isNotNull();
        assertThat(response.getClasses()).isNotEmpty();
        assertThat(response.getCurrency()).isEqualTo("XOF");
        verify(tenantValidationService, org.mockito.Mockito.atLeastOnce()).validateSchoolAccess(schoolId);
    }
}
