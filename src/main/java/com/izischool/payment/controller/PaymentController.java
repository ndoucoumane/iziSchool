package com.izischool.payment.controller;

import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.response.PageResponse;
import com.izischool.common.util.ReferenceGenerator;
import com.izischool.parent.domain.Parent;
import com.izischool.parent.service.ParentService;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import com.izischool.payment.dto.CancelPaymentRequest;
import com.izischool.payment.dto.CreatePaymentRequest;
import com.izischool.payment.dto.InitiateMobileMoneyRequest;
import com.izischool.payment.dto.MobileMoneyInitResponse;
import com.izischool.payment.dto.PaymentResponse;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.payment.service.PaymentService;
import com.izischool.school.domain.School;
import com.izischool.student.domain.Student;
import com.izischool.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Gestion des transactions de paiement (comptoir, virement, mobile money)")
@SecurityRequirement(name = "BearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final StudentService studentService;
    private final ParentService parentService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Lister les paiements", description = "Retourne l'historique paginé des transactions financières")
    @ApiResponse(responseCode = "200", description = "Liste paginée des paiements")
    @GetMapping
    public ResponseEntity<PageResponse<PaymentResponse>> listPayments(
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) String reference,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Page<Payment> paymentPage = paymentService.getPaymentsBySchool(schoolId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PageResponse.of(paymentPage, PaymentResponse::fromEntity));
    }

    @Operation(summary = "Enregistrer un paiement manuel", description = "Enregistre un paiement en espèces, chèque ou virement avec affectation aux échéances")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paiement enregistré et reçu généré avec succès"),
            @ApiResponse(responseCode = "400", description = "Montant supérieur au solde dû ou données invalides")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        School school = currentUserContextService.getCurrentSchool()
                .orElseThrow(() -> new IllegalStateException("School context required"));

        Student student = studentService.getStudentById(request.getStudentId(), schoolId);
        Parent parent = request.getParentId() != null ? parentService.getParentById(request.getParentId(), schoolId) : null;

        String txId = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : (request.getReference() != null && !request.getReference().isBlank() ? request.getReference() : null);

        PaymentMethod method = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CASH;
        PaymentProvider provider = request.getProvider();
        if (provider == null) {
            provider = switch (method) {
                case ORANGE_MONEY -> PaymentProvider.ORANGE_MONEY;
                case WAVE -> PaymentProvider.WAVE;
                case MOBILE_MONEY -> PaymentProvider.WAVE;
                default -> PaymentProvider.MANUAL;
            };
        }

        Payment payment = Payment.builder()
                .school(school)
                .student(student)
                .parent(parent)
                .amount(request.getAmount())
                .currency(school.getCurrency())
                .paymentMethod(method)
                .provider(provider)
                .providerTransactionId(txId)
                .paymentDate(Instant.now())
                .status(PaymentStatus.PENDING)
                .description(request.getNote())
                .build();

        Map<UUID, BigDecimal> allocations = request.getScheduleId() != null
                ? Map.of(request.getScheduleId(), request.getAmount())
                : null;

        Payment processed = paymentService.processPaymentWithAllocations(payment, allocations);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.fromEntity(processed));
    }

    @Operation(summary = "Détails d'un paiement", description = "Récupère les informations détaillées d'une transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paiement trouvé"),
            @ApiResponse(responseCode = "404", description = "Paiement non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Payment payment = paymentService.getPaymentById(id, schoolId);
        return ResponseEntity.ok(PaymentResponse.fromEntity(payment));
    }

    @Operation(summary = "Annuler un paiement", description = "Annule la transaction, réajuste les échéances et invalide le reçu")
    @ApiResponse(responseCode = "200", description = "Paiement annulé")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable UUID id,
            @Valid @RequestBody CancelPaymentRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Payment cancelled = paymentService.cancelPayment(id, schoolId, request.getReason());
        return ResponseEntity.ok(PaymentResponse.fromEntity(cancelled));
    }

    @Operation(summary = "Initier un paiement Mobile Money (Wave / Orange Money)", description = "Crée une session de paiement mobile et retourne le lien de paiement")
    @ApiResponse(responseCode = "200", description = "Paiement initié avec URL de redirection")
    @PostMapping("/mobile-money")
    public ResponseEntity<MobileMoneyInitResponse> initiateMobileMoney(
            @Valid @RequestBody InitiateMobileMoneyRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        School school = currentUserContextService.getCurrentSchool()
                .orElseThrow(() -> new IllegalStateException("School context required"));

        Student student = studentService.getStudentById(request.getStudentId(), schoolId);
        Parent parent = request.getParentId() != null ? parentService.getParentById(request.getParentId(), schoolId) : null;

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
                .description("Mobile Money initiated via " + request.getProvider())
                .build();

        Payment saved = paymentRepository.save(payment);

        MobileMoneyInitResponse response = MobileMoneyInitResponse.builder()
                .paymentId(saved.getId())
                .status(PaymentStatus.PENDING)
                .provider(request.getProvider())
                .paymentUrl(mockCheckoutUrl)
                .reference(ref)
                .build();

        return ResponseEntity.ok(response);
    }
}
