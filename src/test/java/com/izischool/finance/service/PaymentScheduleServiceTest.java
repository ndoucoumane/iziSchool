package com.izischool.finance.service;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.AcademicYearStatus;
import com.izischool.academic.domain.GradeLevel;
import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.domain.SchoolClassStatus;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeType;
import com.izischool.finance.domain.PaymentFrequency;
import com.izischool.finance.domain.PaymentPlan;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.student.domain.EnrollmentStatus;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentEnrollment;
import com.izischool.student.domain.StudentStatus;
import com.izischool.tenant.service.TenantValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentScheduleServiceTest {

    @Mock
    private PaymentScheduleRepository paymentScheduleRepository;

    @Mock
    private TenantValidationService tenantValidationService;

    @InjectMocks
    private PaymentScheduleService paymentScheduleService;

    private School school;
    private AcademicYear academicYear;
    private Student student;
    private StudentEnrollment enrollment;
    private Fee fee;
    private PaymentPlan monthlyPlan;

    @BeforeEach
    void setUp() {
        school = School.builder()
                .id(UUID.randomUUID())
                .name("Collège Moderne")
                .code("COL-MOD")
                .currency("XOF")
                .status(SchoolStatus.ACTIVE)
                .build();

        academicYear = AcademicYear.builder()
                .id(UUID.randomUUID())
                .school(school)
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2027, 6, 30))
                .status(AcademicYearStatus.ACTIVE)
                .build();

        GradeLevel grade = GradeLevel.builder()
                .id(UUID.randomUUID())
                .school(school)
                .name("6ème")
                .code("6EME")
                .displayOrder(1)
                .build();

        SchoolClass schoolClass = SchoolClass.builder()
                .id(UUID.randomUUID())
                .school(school)
                .academicYear(academicYear)
                .gradeLevel(grade)
                .name("6ème A")
                .code("6A")
                .capacity(35)
                .status(SchoolClassStatus.ACTIVE)
                .build();

        student = Student.builder()
                .id(UUID.randomUUID())
                .school(school)
                .studentNumber("COL-STD-0001")
                .firstName("Awa")
                .lastName("Fall")
                .gender(Gender.FEMALE)
                .status(StudentStatus.ACTIVE)
                .build();

        enrollment = StudentEnrollment.builder()
                .id(UUID.randomUUID())
                .school(school)
                .student(student)
                .academicYear(academicYear)
                .schoolClass(schoolClass)
                .enrollmentDate(LocalDate.of(2026, 9, 15))
                .status(EnrollmentStatus.ACTIVE)
                .build();

        fee = Fee.builder()
                .id(UUID.randomUUID())
                .school(school)
                .academicYear(academicYear)
                .name("Scolarité 6ème")
                .code("SCOL-6EME")
                .amount(new BigDecimal("600000.00"))
                .currency("XOF")
                .feeType(FeeType.TUITION)
                .build();

        monthlyPlan = PaymentPlan.builder()
                .id(UUID.randomUUID())
                .school(school)
                .academicYear(academicYear)
                .name("Plan 10 mensualités")
                .numberOfInstallments(10)
                .frequency(PaymentFrequency.MONTHLY)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Génération de 10 mensualités réparties équitablement avec dates séquentielles")
    void testGenerateSchedulesFromPlan() {
        when(paymentScheduleRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<PaymentSchedule> schedules = paymentScheduleService.generateSchedulesFromPlan(
                enrollment,
                fee,
                monthlyPlan,
                new BigDecimal("600000.00"),
                LocalDate.of(2026, 10, 1)
        );

        assertThat(schedules).hasSize(10);
        BigDecimal totalSum = schedules.stream()
                .map(PaymentSchedule::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalSum).isEqualByComparingTo("600000.00");
        assertThat(schedules.get(0).getAmountDue()).isEqualByComparingTo("60000.00");
        assertThat(schedules.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(schedules.get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(schedules.get(9).getDueDate()).isEqualTo(LocalDate.of(2027, 7, 1));

        verify(paymentScheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Recalcul de statut PaymentSchedule : transition de PENDING vers OVERDUE")
    void testScheduleStatusOverdueTransition() {
        PaymentSchedule overdueSchedule = PaymentSchedule.builder()
                .dueDate(LocalDate.now().minusDays(5))
                .amountDue(new BigDecimal("50000.00"))
                .amountPaid(BigDecimal.ZERO)
                .status(PaymentScheduleStatus.PENDING)
                .build();

        overdueSchedule.recalculateStatus(LocalDate.now());

        assertThat(overdueSchedule.getStatus()).isEqualTo(PaymentScheduleStatus.OVERDUE);
        assertThat(overdueSchedule.getRemainingAmount()).isEqualByComparingTo("50000.00");
    }
}
