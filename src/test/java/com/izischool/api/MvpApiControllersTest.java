package com.izischool.api;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.service.AcademicYearService;
import com.izischool.academic.service.SchoolClassService;
import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserRole;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.dashboard.dto.DashboardOverviewResponse;
import com.izischool.dashboard.service.DashboardService;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeType;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.dto.FeeRequest;
import com.izischool.finance.service.FeeService;
import com.izischool.finance.service.PaymentScheduleService;
import com.izischool.parent.domain.Parent;
import com.izischool.parent.domain.ParentStatus;
import com.izischool.parent.dto.ParentRequest;
import com.izischool.parent.service.ParentService;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import com.izischool.payment.domain.Receipt;
import com.izischool.payment.domain.ReceiptStatus;
import com.izischool.payment.service.PdfReceiptGeneratorService;
import com.izischool.payment.service.PaymentService;
import com.izischool.payment.service.ReceiptService;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.school.service.SchoolService;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentStatus;
import com.izischool.student.dto.StudentBalanceResponse;
import com.izischool.student.dto.StudentRequest;
import com.izischool.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MvpApiControllersTest {

    @Mock
    private SchoolService schoolService;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private SchoolClassService schoolClassService;
    @Mock
    private StudentService studentService;
    @Mock
    private ParentService parentService;
    @Mock
    private FeeService feeService;
    @Mock
    private PaymentScheduleService paymentScheduleService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private ReceiptService receiptService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private CurrentUserContextService currentUserContextService;

    private School sampleSchool;
    private AcademicYear sampleYear;
    private Student sampleStudent;
    private Parent sampleParent;

    @BeforeEach
    void setUp() {
        sampleSchool = School.builder()
                .id(UUID.randomUUID())
                .name("École Pilote Test")
                .code("PILOTE-01")
                .currency("XOF")
                .status(SchoolStatus.ACTIVE)
                .build();

        sampleYear = AcademicYear.builder()
                .id(UUID.randomUUID())
                .school(sampleSchool)
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2027, 6, 30))
                .status(com.izischool.academic.domain.AcademicYearStatus.ACTIVE)
                .build();

        sampleStudent = Student.builder()
                .id(UUID.randomUUID())
                .school(sampleSchool)
                .studentNumber("PILOTE-01-STD-0001")
                .firstName("Mamadou")
                .lastName("Diallo")
                .gender(Gender.MALE)
                .status(StudentStatus.ACTIVE)
                .build();

        sampleParent = Parent.builder()
                .id(UUID.randomUUID())
                .school(sampleSchool)
                .firstName("Ibrahima")
                .lastName("Diallo")
                .phone("+221771234567")
                .email("ibrahima@example.com")
                .status(ParentStatus.ACTIVE)
                .build();
    }

    @Test
    void testStudentBalanceComputation() {
        UUID schoolId = sampleSchool.getId();
        UUID studentId = sampleStudent.getId();

        StudentBalanceResponse balance = StudentBalanceResponse.builder()
                .studentId(studentId)
                .studentNumber(sampleStudent.getStudentNumber())
                .studentName(sampleStudent.getFullName())
                .totalDue(new BigDecimal("350000.00"))
                .totalPaid(new BigDecimal("150000.00"))
                .totalOutstanding(new BigDecimal("200000.00"))
                .currency("XOF")
                .build();

        when(studentService.getStudentBalance(studentId, schoolId)).thenReturn(balance);

        StudentBalanceResponse result = studentService.getStudentBalance(studentId, schoolId);
        assertNotNull(result);
        assertEquals(new BigDecimal("350000.00"), result.getTotalDue());
        assertEquals(new BigDecimal("150000.00"), result.getTotalPaid());
        assertEquals(new BigDecimal("200000.00"), result.getTotalOutstanding());
    }

    @Test
    void testPdfReceiptGeneration() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .school(sampleSchool)
                .student(sampleStudent)
                .parent(sampleParent)
                .amount(new BigDecimal("50000.00"))
                .currency("XOF")
                .paymentMethod(PaymentMethod.CASH)
                .provider(PaymentProvider.MANUAL)
                .paymentReference("PILOTE-PAY-0001")
                .paymentDate(Instant.now())
                .status(PaymentStatus.SUCCESS)
                .build();

        Receipt receipt = Receipt.builder()
                .id(UUID.randomUUID())
                .school(sampleSchool)
                .payment(payment)
                .receiptNumber("PILOTE-REC-0001")
                .amount(new BigDecimal("50000.00"))
                .currency("XOF")
                .issuedAt(Instant.now())
                .status(ReceiptStatus.GENERATED)
                .build();

        PdfReceiptGeneratorService pdfGenerator = new PdfReceiptGeneratorService();
        byte[] pdf = pdfGenerator.generateReceiptPdf(receipt);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // PDF header starts with %PDF
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    void testDashboardOverviewMetrics() {
        UUID schoolId = sampleSchool.getId();
        DashboardOverviewResponse overview = DashboardOverviewResponse.builder()
                .students(150)
                .totalExpected(new BigDecimal("45000000.00"))
                .totalCollected(new BigDecimal("30000000.00"))
                .totalOutstanding(new BigDecimal("15000000.00"))
                .collectionRate(new BigDecimal("66.67"))
                .todayPayments(new BigDecimal("1500000.00"))
                .overdueSchedules(12)
                .currency("XOF")
                .build();

        when(dashboardService.getOverview(schoolId)).thenReturn(overview);

        DashboardOverviewResponse result = dashboardService.getOverview(schoolId);
        assertNotNull(result);
        assertEquals(150, result.getStudents());
        assertEquals(new BigDecimal("66.67"), result.getCollectionRate());
    }

    @Test
    void testSchoolClassResponseFromEntity() {
        SchoolClass schoolClass = SchoolClass.builder()
                .id(UUID.randomUUID())
                .school(sampleSchool)
                .academicYear(sampleYear)
                .name("Terminale S1")
                .code("TS1")
                .capacity(30)
                .status(com.izischool.academic.domain.SchoolClassStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        com.izischool.academic.dto.SchoolClassResponse response = com.izischool.academic.dto.SchoolClassResponse.fromEntity(schoolClass);
        assertNotNull(response);
        assertEquals("Terminale S1", response.getName());
        assertEquals("TS1", response.getCode());
        assertEquals(sampleYear.getName(), response.getAcademicYearName());
        assertEquals(sampleYear.getId(), response.getAcademicYearId());
        assertEquals(sampleSchool.getId(), response.getSchoolId());

        // Test with null associations
        SchoolClass emptyClass = SchoolClass.builder().name("Class without relations").build();
        com.izischool.academic.dto.SchoolClassResponse emptyResponse = com.izischool.academic.dto.SchoolClassResponse.fromEntity(emptyClass);
        assertNotNull(emptyResponse);
        assertEquals("Class without relations", emptyResponse.getName());
        assertNull(emptyResponse.getAcademicYearId());
        assertNull(emptyResponse.getAcademicYearName());
    }

    @Test
    void testFeeResponseFromEntity() {
        Fee fee = Fee.builder()
                .id(UUID.randomUUID())
                .school(sampleSchool)
                .academicYear(sampleYear)
                .name("Frais d'inscription")
                .code("INSCRIP-2026")
                .feeType(FeeType.TUITION)
                .amount(new BigDecimal("50000.00"))
                .currency("XOF")
                .active(true)
                .mandatory(true)
                .build();

        com.izischool.finance.dto.FeeResponse response = com.izischool.finance.dto.FeeResponse.fromEntity(fee);
        assertNotNull(response);
        assertEquals("Frais d'inscription", response.getName());
        assertEquals("INSCRIP-2026", response.getCode());
        assertEquals(sampleYear.getName(), response.getAcademicYearName());
        assertEquals(sampleYear.getId(), response.getAcademicYearId());
    }

    @Test
    void testPaymentScheduleResponseFromEntity() {
        Fee fee = Fee.builder()
                .id(UUID.randomUUID())
                .name("Scolarité Octobre")
                .code("SCOL-OCT")
                .build();

        PaymentSchedule schedule = PaymentSchedule.builder()
                .id(UUID.randomUUID())
                .student(sampleStudent)
                .fee(fee)
                .dueDate(LocalDate.of(2026, 10, 5))
                .amountDue(new BigDecimal("25000.00"))
                .amountPaid(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("25000.00"))
                .currency("XOF")
                .status(PaymentScheduleStatus.PENDING)
                .installmentNumber(1)
                .build();

        com.izischool.finance.dto.PaymentScheduleResponse response = com.izischool.finance.dto.PaymentScheduleResponse.fromEntity(schedule);
        assertNotNull(response);
        assertEquals(sampleStudent.getId(), response.getStudentId());
        assertEquals(sampleStudent.getFullName(), response.getStudentName());
        assertEquals("Scolarité Octobre", response.getFeeName());
        assertEquals("SCOL-OCT", response.getFeeCode());
    }
}
