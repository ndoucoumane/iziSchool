package com.izischool.integration;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.AcademicYearStatus;
import com.izischool.academic.domain.GradeLevel;
import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.domain.SchoolClassStatus;
import com.izischool.academic.service.AcademicYearService;
import com.izischool.academic.service.GradeLevelService;
import com.izischool.academic.service.SchoolClassService;
import com.izischool.audit.repository.AuditLogRepository;
import com.izischool.dashboard.dto.DashboardStatsDto;
import com.izischool.dashboard.service.DashboardService;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeType;
import com.izischool.finance.domain.PaymentFrequency;
import com.izischool.finance.domain.PaymentPlan;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.service.FeeService;
import com.izischool.finance.service.PaymentPlanService;
import com.izischool.finance.service.PaymentScheduleService;
import com.izischool.parent.domain.Parent;
import com.izischool.parent.domain.ParentRelationship;
import com.izischool.parent.domain.ParentStatus;
import com.izischool.parent.service.ParentService;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import com.izischool.payment.domain.Receipt;
import com.izischool.payment.repository.ReceiptRepository;
import com.izischool.payment.service.PaymentService;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.school.service.SchoolService;
import com.izischool.student.domain.EnrollmentStatus;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentEnrollment;
import com.izischool.student.domain.StudentStatus;
import com.izischool.student.service.StudentEnrollmentService;
import com.izischool.student.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialWorkflowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private AcademicYearService academicYearService;

    @Autowired
    private GradeLevelService gradeLevelService;

    @Autowired
    private SchoolClassService schoolClassService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ParentService parentService;

    @Autowired
    private StudentEnrollmentService enrollmentService;

    @Autowired
    private FeeService feeService;

    @Autowired
    private PaymentPlanService paymentPlanService;

    @Autowired
    private PaymentScheduleService paymentScheduleService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private DashboardService dashboardService;

    @Test
    @DisplayName("Workflow complet de bout-en-bout : Inscription, Échéancier, Paiement, Allocation, Reçu et Dashboard")
    void testCompleteFinancialWorkflow() {
        // 1. Création de l'école (Tenant)
        School school = schoolService.createSchool(School.builder()
                .name("Institution Notre Dame")
                .code("NOTRE-DAME-" + System.currentTimeMillis())
                .currency("XOF")
                .status(SchoolStatus.ACTIVE)
                .build());
        assertThat(school.getId()).isNotNull();

        // 2. Année scolaire
        AcademicYear academicYear = academicYearService.createAcademicYear(AcademicYear.builder()
                .school(school)
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2027, 6, 30))
                .status(AcademicYearStatus.ACTIVE)
                .build());
        assertThat(academicYear.getId()).isNotNull();

        // 3. Niveau et classe
        GradeLevel grade = gradeLevelService.createGradeLevel(GradeLevel.builder()
                .school(school)
                .name("6ème")
                .code("6EME")
                .displayOrder(1)
                .build());

        SchoolClass schoolClass = schoolClassService.createSchoolClass(SchoolClass.builder()
                .school(school)
                .academicYear(academicYear)
                .gradeLevel(grade)
                .name("6ème B")
                .code("6B")
                .capacity(30)
                .status(SchoolClassStatus.ACTIVE)
                .build());

        // 4. Élève et Parent
        Student student = studentService.createStudent(Student.builder()
                .school(school)
                .firstName("Khadija")
                .lastName("Ba")
                .gender(Gender.FEMALE)
                .status(StudentStatus.ACTIVE)
                .build());
        assertThat(student.getStudentNumber()).isNotNull();

        Parent parent = parentService.createParent(Parent.builder()
                .school(school)
                .firstName("Ousmane")
                .lastName("Ba")
                .phone("+221770000000")
                .email("ousmane.ba@gmail.com")
                .status(ParentStatus.ACTIVE)
                .build());

        parentService.linkStudentAndParent(student, parent, ParentRelationship.FATHER, true, true);

        // 5. Inscription
        StudentEnrollment enrollment = enrollmentService.enrollStudent(StudentEnrollment.builder()
                .school(school)
                .student(student)
                .academicYear(academicYear)
                .schoolClass(schoolClass)
                .enrollmentDate(LocalDate.now())
                .status(EnrollmentStatus.ACTIVE)
                .build());

        // 6. Frais et Plan de paiement
        Fee fee = feeService.createFee(Fee.builder()
                .school(school)
                .academicYear(academicYear)
                .name("Scolarité Annuelle 6ème")
                .code("SCOL-6B")
                .amount(new BigDecimal("500000.00"))
                .currency("XOF")
                .feeType(FeeType.TUITION)
                .build());

        PaymentPlan plan = paymentPlanService.createPaymentPlan(PaymentPlan.builder()
                .school(school)
                .academicYear(academicYear)
                .name("Plan 5 versements")
                .numberOfInstallments(5)
                .frequency(PaymentFrequency.MONTHLY)
                .build());

        // 7. Génération de l'échéancier (5 versements de 100 000 FCFA)
        List<PaymentSchedule> schedules = paymentScheduleService.generateSchedulesFromPlan(
                enrollment,
                fee,
                plan,
                new BigDecimal("500000.00"),
                LocalDate.of(2026, 10, 5)
        );
        assertThat(schedules).hasSize(5);
        assertThat(schedules.get(0).getAmountDue()).isEqualByComparingTo("100000.00");
        assertThat(schedules.get(0).getStatus()).isEqualTo(PaymentScheduleStatus.PENDING);

        // 8. Paiement de 150 000 FCFA (couvre l'échéance 1 à 100 000 et l'échéance 2 à 50 000)
        Payment payment = Payment.builder()
                .school(school)
                .student(student)
                .parent(parent)
                .amount(new BigDecimal("150000.00"))
                .currency("XOF")
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .provider(PaymentProvider.WAVE)
                .providerTransactionId("WAVE-INT-TEST-001")
                .status(PaymentStatus.PENDING)
                .build();

        Payment processed = paymentService.processPayment(payment);

        assertThat(processed.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // 9. Vérification des échéances
        List<PaymentSchedule> updatedSchedules = paymentScheduleService.getSchedulesByStudent(school.getId(), student.getId());
        assertThat(updatedSchedules).hasSize(5);

        // Échéance 1 : Complètement payée
        PaymentSchedule sched1 = updatedSchedules.get(0);
        assertThat(sched1.getAmountPaid()).isEqualByComparingTo("100000.00");
        assertThat(sched1.getRemainingAmount()).isEqualByComparingTo("0.00");
        assertThat(sched1.getStatus()).isEqualTo(PaymentScheduleStatus.PAID);

        // Échéance 2 : Partiellement payée
        PaymentSchedule sched2 = updatedSchedules.get(1);
        assertThat(sched2.getAmountPaid()).isEqualByComparingTo("50000.00");
        assertThat(sched2.getRemainingAmount()).isEqualByComparingTo("50000.00");
        assertThat(sched2.getStatus()).isEqualTo(PaymentScheduleStatus.PARTIALLY_PAID);

        // Échéance 3 : Toujours PENDING
        PaymentSchedule sched3 = updatedSchedules.get(2);
        assertThat(sched3.getAmountPaid()).isEqualByComparingTo("0.00");
        assertThat(sched3.getRemainingAmount()).isEqualByComparingTo("100000.00");
        assertThat(sched3.getStatus()).isEqualTo(PaymentScheduleStatus.PENDING);

        // 10. Vérification du reçu
        Optional<Receipt> receiptOpt = receiptRepository.findByPayment_Id(processed.getId());
        assertThat(receiptOpt).isPresent();
        Receipt receipt = receiptOpt.get();
        assertThat(receipt.getAmount()).isEqualByComparingTo("150000.00");
        assertThat(receipt.getReceiptNumber()).isNotNull();

        // 11. Vérification du Dashboard
        DashboardStatsDto dashboard = dashboardService.getDashboardMetrics(school.getId());
        assertThat(dashboard.getTotalStudents()).isEqualTo(1L);
        assertThat(dashboard.getTotalExpectedAmount()).isEqualByComparingTo("500000.00");
        assertThat(dashboard.getTotalCollectedAmount()).isEqualByComparingTo("150000.00");
        assertThat(dashboard.getTotalOutstandingAmount()).isEqualByComparingTo("350000.00");
        assertThat(dashboard.getCollectionRate()).isEqualByComparingTo("30.00");
    }
}
