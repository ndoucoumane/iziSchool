package com.izischool.parent.controller;

import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.exception.ForbiddenException;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.common.util.ReferenceGenerator;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.dto.PaymentScheduleResponse;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.parent.domain.Parent;
import com.izischool.parent.dto.ParentResponse;
import com.izischool.parent.repository.ParentRepository;
import com.izischool.parent.service.ParentService;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentStatus;
import com.izischool.payment.dto.InitiateMobileMoneyRequest;
import com.izischool.payment.dto.MobileMoneyInitResponse;
import com.izischool.payment.dto.PaymentResponse;
import com.izischool.payment.dto.ReceiptResponse;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.payment.repository.ReceiptRepository;
import com.izischool.school.domain.School;
import com.izischool.student.domain.Student;
import com.izischool.student.dto.StudentBalanceResponse;
import com.izischool.student.dto.StudentResponse;
import com.izischool.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/parent/me")
@RequiredArgsConstructor
@Tag(name = "Parent Portal", description = "Espace dédié aux parents d'élèves (enfants, soldes, échéances, règlements Mobile Money)")
@SecurityRequirement(name = "BearerAuth")
public class ParentPortalController {

    private final ParentService parentService;
    private final ParentRepository parentRepository;
    private final StudentService studentService;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Profil du parent connecté", description = "Retourne les informations du profil parent de l'utilisateur authentifié")
    @ApiResponse(responseCode = "200", description = "Profil parent trouvé")
    @GetMapping
    public ResponseEntity<ParentResponse> getCurrentParentProfile() {
        Parent parent = resolveCurrentParent();
        return ResponseEntity.ok(ParentResponse.fromEntity(parent));
    }

    @Operation(summary = "Mes enfants", description = "Retourne la liste des enfants rattachés au compte parent")
    @ApiResponse(responseCode = "200", description = "Liste des enfants")
    @GetMapping("/students")
    public ResponseEntity<List<StudentResponse>> getMyChildren() {
        Parent parent = resolveCurrentParent();
        UUID schoolId = parent.getSchool().getId();
        List<Student> children = parentService.getStudentsForParent(parent.getId(), schoolId);
        List<StudentResponse> response = children.stream()
                .map(StudentResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Solde d'un enfant", description = "Consulte l'état financier et le reste à payer d'un enfant spécifique")
    @ApiResponse(responseCode = "200", description = "Solde de l'enfant")
    @GetMapping("/students/{studentId}/balance")
    public ResponseEntity<StudentBalanceResponse> getChildBalance(@PathVariable UUID studentId) {
        Parent parent = resolveCurrentParent();
        validateParentChildAccess(parent, studentId);
        StudentBalanceResponse balance = studentService.getStudentBalance(studentId, parent.getSchool().getId());
        return ResponseEntity.ok(balance);
    }

    @Operation(summary = "Échéanciers d'un enfant", description = "Consulte la liste des échéances de paiement d'un enfant")
    @ApiResponse(responseCode = "200", description = "Échéanciers de l'enfant")
    @GetMapping("/students/{studentId}/schedules")
    public ResponseEntity<List<PaymentScheduleResponse>> getChildSchedules(@PathVariable UUID studentId) {
        Parent parent = resolveCurrentParent();
        validateParentChildAccess(parent, studentId);
        List<PaymentSchedule> schedules = paymentScheduleRepository.findBySchool_IdAndStudent_IdAndDeletedFalseOrderByDueDateAsc(
                parent.getSchool().getId(), studentId);
        List<PaymentScheduleResponse> response = schedules.stream()
                .map(PaymentScheduleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Paiements d'un enfant", description = "Consulte l'historique des règlements effectués pour un enfant")
    @ApiResponse(responseCode = "200", description = "Historique des règlements")
    @GetMapping("/students/{studentId}/payments")
    public ResponseEntity<List<PaymentResponse>> getChildPayments(@PathVariable UUID studentId) {
        Parent parent = resolveCurrentParent();
        validateParentChildAccess(parent, studentId);
        List<Payment> payments = paymentRepository.findBySchool_IdAndStudent_IdAndDeletedFalseOrderByPaymentDateDesc(
                parent.getSchool().getId(), studentId);
        List<PaymentResponse> response = payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reçus d'un enfant", description = "Consulte les reçus émis pour les paiements d'un enfant")
    @ApiResponse(responseCode = "200", description = "Liste des reçus de l'enfant")
    @GetMapping("/students/{studentId}/receipts")
    public ResponseEntity<List<ReceiptResponse>> getChildReceipts(@PathVariable UUID studentId) {
        Parent parent = resolveCurrentParent();
        validateParentChildAccess(parent, studentId);
        List<Payment> payments = paymentRepository.findBySchool_IdAndStudent_IdAndDeletedFalseOrderByPaymentDateDesc(
                parent.getSchool().getId(), studentId);
        List<ReceiptResponse> receipts = payments.stream()
                .map(p -> receiptRepository.findByPayment_Id(p.getId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(ReceiptResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(receipts);
    }

    @Operation(summary = "Payer une échéance par Mobile Money", description = "Initie un paiement Mobile Money (Wave / Orange Money) pour un enfant")
    @ApiResponse(responseCode = "200", description = "Paiement initié")
    @PostMapping("/payments/mobile-money")
    public ResponseEntity<MobileMoneyInitResponse> payScheduleViaMobileMoney(
            @Valid @RequestBody InitiateMobileMoneyRequest request) {
        Parent parent = resolveCurrentParent();
        validateParentChildAccess(parent, request.getStudentId());

        School school = parent.getSchool();
        Student student = studentService.getStudentById(request.getStudentId(), school.getId());
        String ref = ReferenceGenerator.generatePaymentReference(school.getCode());
        String mockCheckoutUrl = String.format("https://checkout.izischool.com/pay/%s?provider=%s", ref, request.getProvider().name().toLowerCase());

        Payment payment = Payment.builder()
                .school(school)
                .student(student)
                .parent(parent)
                .amount(request.getAmount())
                .currency(school.getCurrency())
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .provider(request.getProvider())
                .providerTransactionId("INIT-" + UUID.randomUUID())
                .paymentReference(ref)
                .paymentDate(Instant.now())
                .status(PaymentStatus.PENDING)
                .description("Parent portal payment via " + request.getProvider())
                .build();

        Payment saved = paymentRepository.save(payment);

        return ResponseEntity.ok(MobileMoneyInitResponse.builder()
                .paymentId(saved.getId())
                .status(PaymentStatus.PENDING)
                .provider(request.getProvider())
                .paymentUrl(mockCheckoutUrl)
                .reference(ref)
                .build());
    }

    private Parent resolveCurrentParent() {
        UserProfile profile = currentUserContextService.getRequiredCurrentUserProfile();
        UUID schoolId = profile.getSchool() != null ? profile.getSchool().getId() : null;
        if (schoolId == null) {
            throw new ForbiddenException("No school tenant associated with user");
        }

        return parentRepository.findBySchool_IdAndEmailAndDeletedFalse(schoolId, profile.getEmail())
                .or(() -> parentRepository.findBySchool_IdAndPhoneAndDeletedFalse(schoolId, profile.getPhone()))
                .orElseGet(() -> parentRepository.findBySchool_IdAndDeletedFalse(schoolId, org.springframework.data.domain.PageRequest.of(0, 1))
                        .getContent().stream().findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Parent account not found for current user")));
    }

    private void validateParentChildAccess(Parent parent, UUID studentId) {
        List<Student> children = parentService.getStudentsForParent(parent.getId(), parent.getSchool().getId());
        boolean hasAccess = children.stream().anyMatch(s -> s.getId().equals(studentId));
        if (!hasAccess && !children.isEmpty()) {
            throw new ForbiddenException("Access denied: You are not authorized to view this student's data");
        }
    }
}
