package com.izischool.student.controller;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.AcademicYearStatus;
import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.domain.SchoolClassStatus;
import com.izischool.academic.service.AcademicYearService;
import com.izischool.academic.service.SchoolClassService;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.response.PageResponse;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeType;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.repository.FeeRepository;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.finance.service.FeeService;
import com.izischool.finance.service.PaymentScheduleService;
import com.izischool.parent.service.ParentService;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.payment.repository.ReceiptRepository;
import com.izischool.payment.service.PaymentService;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.student.domain.EnrollmentStatus;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentEnrollment;
import com.izischool.student.domain.StudentStatus;
import com.izischool.student.dto.StudentRequest;
import com.izischool.student.dto.StudentResponse;
import com.izischool.student.dto.UpdateStudentStatusRequest;
import com.izischool.student.repository.StudentEnrollmentRepository;
import com.izischool.student.service.StudentEnrollmentService;
import com.izischool.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private StudentService studentService;
    @Mock
    private StudentEnrollmentService studentEnrollmentService;
    @Mock
    private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock
    private SchoolClassService schoolClassService;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private ParentService parentService;
    @Mock
    private FeeService feeService;
    @Mock
    private FeeRepository feeRepository;
    @Mock
    private PaymentScheduleService paymentScheduleService;
    @Mock
    private PaymentScheduleRepository paymentScheduleRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private CurrentUserContextService currentUserContextService;

    @InjectMocks
    private StudentController studentController;

    private School school;
    private AcademicYear academicYear;
    private SchoolClass schoolClass;
    private Student student;
    private StudentEnrollment enrollment;

    @BeforeEach
    void setUp() {
        school = School.builder()
                .id(UUID.randomUUID())
                .name("École Pilote Test")
                .code("PILOTE-01")
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

        schoolClass = SchoolClass.builder()
                .id(UUID.randomUUID())
                .school(school)
                .academicYear(academicYear)
                .name("6ème A")
                .code("6A")
                .capacity(35)
                .status(SchoolClassStatus.ACTIVE)
                .build();

        student = Student.builder()
                .id(UUID.randomUUID())
                .school(school)
                .studentNumber("PILOTE-01-STD-0001")
                .firstName("Mamadou")
                .lastName("Diallo")
                .gender(Gender.MALE)
                .status(StudentStatus.ACTIVE)
                .build();

        enrollment = StudentEnrollment.builder()
                .id(UUID.randomUUID())
                .school(school)
                .student(student)
                .academicYear(academicYear)
                .schoolClass(schoolClass)
                .enrollmentDate(LocalDate.now())
                .status(EnrollmentStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Création d'un élève avec scolarité totale et versement initial partiel")
    void testCreateStudentWithTuitionAndPartialPayment() {
        UUID schoolId = school.getId();
        when(currentUserContextService.getRequiredSchoolId()).thenReturn(schoolId);
        when(currentUserContextService.getCurrentSchool()).thenReturn(Optional.of(school));
        when(studentService.createStudent(any(Student.class))).thenReturn(student);
        when(schoolClassService.getSchoolClassById(schoolClass.getId(), schoolId)).thenReturn(schoolClass);
        when(academicYearService.getActiveAcademicYear(schoolId)).thenReturn(Optional.of(academicYear));
        when(studentEnrollmentService.enrollStudent(any(StudentEnrollment.class))).thenReturn(enrollment);

        Fee fee = Fee.builder()
                .id(UUID.randomUUID())
                .school(school)
                .academicYear(academicYear)
                .name("Frais de scolarité - 6ème A")
                .feeType(FeeType.TUITION)
                .amount(new BigDecimal("350000.00"))
                .currency("XOF")
                .build();

        when(feeRepository.findBySchool_IdAndAcademicYear_IdAndFeeTypeAndActiveTrue(
                schoolId, academicYear.getId(), FeeType.TUITION)).thenReturn(List.of(fee));

        PaymentSchedule schedule = PaymentSchedule.builder()
                .id(UUID.randomUUID())
                .school(school)
                .student(student)
                .fee(fee)
                .amountDue(new BigDecimal("350000.00"))
                .amountPaid(new BigDecimal("150000.00"))
                .remainingAmount(new BigDecimal("200000.00"))
                .status(PaymentScheduleStatus.PARTIALLY_PAID)
                .build();

        when(paymentScheduleService.generateSchedulesDirect(eq(student), eq(enrollment), eq(fee), eq(1), eq(new BigDecimal("350000.00")), any(LocalDate.class)))
                .thenReturn(List.of(schedule));

        when(studentEnrollmentRepository.findBySchool_Id(schoolId)).thenReturn(List.of(enrollment));
        when(paymentScheduleRepository.sumStudentTotalExpectedAmount(schoolId, student.getId())).thenReturn(new BigDecimal("350000.00"));
        when(paymentScheduleRepository.sumStudentTotalCollectedAmount(schoolId, student.getId())).thenReturn(new BigDecimal("150000.00"));
        when(paymentScheduleRepository.sumStudentTotalOutstandingAmount(schoolId, student.getId())).thenReturn(new BigDecimal("200000.00"));

        StudentRequest request = StudentRequest.builder()
                .firstName("Mamadou")
                .lastName("Diallo")
                .dateOfBirth(LocalDate.of(2012, 5, 15))
                .placeOfBirth("Dakar")
                .gender(Gender.MALE)
                .classId(schoolClass.getId())
                .totalDue(new BigDecimal("350000.00"))
                .initialPayment(new BigDecimal("150000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        ResponseEntity<StudentResponse> response = studentController.createStudent(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFirstName()).isEqualTo("Mamadou");
        assertThat(response.getBody().getClassName()).isEqualTo("6ème A");
        assertThat(response.getBody().getTotalDue()).isEqualByComparingTo("350000.00");
        assertThat(response.getBody().getTotalPaid()).isEqualByComparingTo("150000.00");
        assertThat(response.getBody().getRemainingAmount()).isEqualByComparingTo("200000.00");
        assertThat(response.getBody().getPaymentStatus()).isEqualTo("PARTIALLY_PAID");

        verify(paymentService).processPayment(any(Payment.class));
    }

    @Test
    @DisplayName("Détails d'un élève avec ses montants financiers et sa classe")
    void testGetStudentByIdWithFinancialAmounts() {
        UUID schoolId = school.getId();
        when(currentUserContextService.getRequiredSchoolId()).thenReturn(schoolId);
        when(studentService.getStudentById(student.getId(), schoolId)).thenReturn(student);
        when(studentEnrollmentRepository.findBySchool_Id(schoolId)).thenReturn(List.of(enrollment));
        when(paymentScheduleRepository.sumStudentTotalExpectedAmount(schoolId, student.getId())).thenReturn(new BigDecimal("350000.00"));
        when(paymentScheduleRepository.sumStudentTotalCollectedAmount(schoolId, student.getId())).thenReturn(new BigDecimal("350000.00"));
        when(paymentScheduleRepository.sumStudentTotalOutstandingAmount(schoolId, student.getId())).thenReturn(BigDecimal.ZERO);

        ResponseEntity<StudentResponse> response = studentController.getStudentById(student.getId());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClassName()).isEqualTo("6ème A");
        assertThat(response.getBody().getTotalDue()).isEqualByComparingTo("350000.00");
        assertThat(response.getBody().getTotalPaid()).isEqualByComparingTo("350000.00");
        assertThat(response.getBody().getRemainingAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getBody().getPaymentStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("Mise à jour du statut d'un élève avec DROPPED_OUT")
    void testUpdateStudentStatusDroppedOut() {
        UUID schoolId = school.getId();
        when(currentUserContextService.getRequiredSchoolId()).thenReturn(schoolId);

        Student droppedOutStudent = Student.builder()
                .id(student.getId())
                .school(school)
                .studentNumber(student.getStudentNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .status(StudentStatus.DROPPED_OUT)
                .build();

        when(studentService.updateStudentStatus(student.getId(), schoolId, StudentStatus.DROPPED_OUT))
                .thenReturn(droppedOutStudent);
        when(studentEnrollmentRepository.findBySchool_Id(schoolId)).thenReturn(List.of(enrollment));

        UpdateStudentStatusRequest request = new UpdateStudentStatusRequest();
        request.setStatus(StudentStatus.DROPPED_OUT);

        ResponseEntity<StudentResponse> response = studentController.updateStudentStatus(student.getId(), request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(StudentStatus.DROPPED_OUT);
    }

    @Test
    @DisplayName("Suppression logique d'un élève (DELETE /api/v1/students/{id})")
    void testDeleteStudent() {
        UUID schoolId = school.getId();
        when(currentUserContextService.getRequiredSchoolId()).thenReturn(schoolId);

        ResponseEntity<Map<String, String>> response = studentController.deleteStudent(student.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "Student deleted successfully");
        verify(studentService).deleteStudent(student.getId(), schoolId);
    }
}
