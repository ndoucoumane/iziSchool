package com.izischool.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.parent.service.ParentService;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import com.izischool.payment.dto.CreatePaymentRequest;
import com.izischool.payment.dto.PaymentResponse;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.payment.service.PaymentService;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentStatus;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private ParentService parentService;

    @Mock
    private CurrentUserContextService currentUserContextService;

    @InjectMocks
    private PaymentController paymentController;

    private School school;
    private Student student;

    @BeforeEach
    void setUp() {
        school = School.builder()
                .id(UUID.randomUUID())
                .name("École Pilote Test")
                .code("PILOTE-01")
                .currency("XOF")
                .status(SchoolStatus.ACTIVE)
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
    }

    @Test
    @DisplayName("Paiement par Orange Money (enum ORANGE_MONEY et provider ORANGE_MONEY)")
    void testCreatePaymentOrangeMoney() {
        UUID schoolId = school.getId();
        when(currentUserContextService.getRequiredSchoolId()).thenReturn(schoolId);
        when(currentUserContextService.getCurrentSchool()).thenReturn(Optional.of(school));
        when(studentService.getStudentById(student.getId(), schoolId)).thenReturn(student);

        Payment processedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .school(school)
                .student(student)
                .amount(new BigDecimal("50000.00"))
                .currency("XOF")
                .paymentMethod(PaymentMethod.ORANGE_MONEY)
                .provider(PaymentProvider.ORANGE_MONEY)
                .paymentReference("PAY-OM-001")
                .status(PaymentStatus.SUCCESS)
                .paymentDate(Instant.now())
                .build();

        when(paymentService.processPaymentWithAllocations(any(Payment.class), any())).thenReturn(processedPayment);

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .studentId(student.getId())
                .amount(new BigDecimal("50000.00"))
                .paymentMethod(PaymentMethod.ORANGE_MONEY)
                .note("Paiement mensualité via Orange Money")
                .build();

        ResponseEntity<PaymentResponse> response = paymentController.createPayment(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPaymentMethod()).isEqualTo(PaymentMethod.ORANGE_MONEY);
        assertThat(response.getBody().getAmount()).isEqualByComparingTo("50000.00");

        verify(paymentService).processPaymentWithAllocations(any(Payment.class), any());
    }

    @Test
    @DisplayName("Désérialisation JSON flexible de PaymentMethod avec aliases (Orange Money, Orange monet, Wave, Chèque)")
    void testPaymentMethodJsonDeserialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String json1 = "{\"studentId\":\"" + UUID.randomUUID() + "\",\"amount\":50000,\"paymentMethod\":\"Orange Money\"}";
        CreatePaymentRequest req1 = objectMapper.readValue(json1, CreatePaymentRequest.class);
        assertThat(req1.getPaymentMethod()).isEqualTo(PaymentMethod.ORANGE_MONEY);

        String json2 = "{\"studentId\":\"" + UUID.randomUUID() + "\",\"amount\":50000,\"paymentMethod\":\"Orange monet\"}";
        CreatePaymentRequest req2 = objectMapper.readValue(json2, CreatePaymentRequest.class);
        assertThat(req2.getPaymentMethod()).isEqualTo(PaymentMethod.ORANGE_MONEY);

        String json3 = "{\"studentId\":\"" + UUID.randomUUID() + "\",\"amount\":50000,\"modePaiement\":\"WAVE\"}";
        CreatePaymentRequest req3 = objectMapper.readValue(json3, CreatePaymentRequest.class);
        assertThat(req3.getPaymentMethod()).isEqualTo(PaymentMethod.WAVE);

        String json4 = "{\"studentId\":\"" + UUID.randomUUID() + "\",\"amount\":50000,\"payment_method\":\"Espèces\"}";
        CreatePaymentRequest req4 = objectMapper.readValue(json4, CreatePaymentRequest.class);
        assertThat(req4.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    }
}
