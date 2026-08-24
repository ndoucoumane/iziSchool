package com.izischool.finance.controller;

import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.response.PageResponse;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.dto.CreatePaymentScheduleRequest;
import com.izischool.finance.dto.GeneratePaymentScheduleRequest;
import com.izischool.finance.dto.PaymentScheduleResponse;
import com.izischool.finance.dto.UpdatePaymentScheduleRequest;
import com.izischool.finance.service.FeeService;
import com.izischool.finance.service.PaymentScheduleService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment-schedules")
@RequiredArgsConstructor
@Tag(name = "Payment Schedules", description = "Gestion et génération des échéanciers de paiement")
@SecurityRequirement(name = "BearerAuth")
public class PaymentScheduleController {

    private final PaymentScheduleService paymentScheduleService;
    private final StudentService studentService;
    private final FeeService feeService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Lister les échéanciers", description = "Retourne la liste paginée des échéances avec filtres")
    @ApiResponse(responseCode = "200", description = "Liste paginée des échéanciers")
    @GetMapping
    public ResponseEntity<PageResponse<PaymentScheduleResponse>> listSchedules(
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) PaymentScheduleStatus status,
            @RequestParam(required = false) LocalDate dueDateFrom,
            @RequestParam(required = false) LocalDate dueDateTo,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Page<PaymentSchedule> schedulesPage = paymentScheduleService.getSchedulesPaged(schoolId, PageRequest.of(page, size, Sort.by("dueDate").ascending()));
        return ResponseEntity.ok(PageResponse.of(schedulesPage, PaymentScheduleResponse::fromEntity));
    }

    @Operation(summary = "Créer une échéance manuelle", description = "Crée une seule échéance pour un élève et un frais donné")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Échéance créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping
    public ResponseEntity<PaymentScheduleResponse> createSchedule(@Valid @RequestBody CreatePaymentScheduleRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Student student = studentService.getStudentById(request.getStudentId(), schoolId);
        Fee fee = feeService.getFeeById(request.getFeeId(), schoolId);

        PaymentSchedule schedule = PaymentSchedule.builder()
                .school(student.getSchool())
                .student(student)
                .fee(fee)
                .dueDate(request.getDueDate())
                .amountDue(request.getAmount())
                .amountPaid(BigDecimal.ZERO)
                .remainingAmount(request.getAmount())
                .currency(fee.getCurrency())
                .status(PaymentScheduleStatus.PENDING)
                .installmentNumber(1)
                .description(request.getDescription() != null ? request.getDescription() : fee.getName())
                .build();

        PaymentSchedule created = paymentScheduleService.createSchedule(schedule);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentScheduleResponse.fromEntity(created));
    }

    @Operation(summary = "Générer un échéancier en tranches", description = "Génère automatiquement les tranches d'échéances réparties sur l'année")
    @ApiResponse(responseCode = "201", description = "Échéances générées avec succès")
    @PostMapping("/generate")
    public ResponseEntity<List<PaymentScheduleResponse>> generateSchedules(@Valid @RequestBody GeneratePaymentScheduleRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Student student = studentService.getStudentById(request.getStudentId(), schoolId);
        Fee fee = feeService.getFeeById(request.getFeeId(), schoolId);

        List<PaymentSchedule> generated = paymentScheduleService.generateSchedulesDirect(
                student,
                fee,
                request.getNumberOfInstallments(),
                request.getTotalAmount(),
                request.getFirstDueDate()
        );

        List<PaymentScheduleResponse> response = generated.stream()
                .map(PaymentScheduleResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Détails d'une échéance", description = "Récupère les détails d'une échéance spécifique")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Échéance trouvée"),
            @ApiResponse(responseCode = "404", description = "Échéance non trouvée")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentScheduleResponse> getScheduleById(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        PaymentSchedule schedule = paymentScheduleService.getScheduleById(id, schoolId);
        return ResponseEntity.ok(PaymentScheduleResponse.fromEntity(schedule));
    }

    @Operation(summary = "Modifier une échéance", description = "Met à jour la date d'échéance ou le montant dû")
    @ApiResponse(responseCode = "200", description = "Échéance mise à jour")
    @PutMapping("/{id}")
    public ResponseEntity<PaymentScheduleResponse> updateSchedule(
            @PathVariable UUID id,
            @RequestBody UpdatePaymentScheduleRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        PaymentSchedule updated = paymentScheduleService.updateSchedule(id, schoolId, request.getDueDate(), request.getAmountDue(), request.getDescription());
        return ResponseEntity.ok(PaymentScheduleResponse.fromEntity(updated));
    }

    @Operation(summary = "Annuler une échéance", description = "Annule l'échéance si aucun paiement n'a encore été enregistré")
    @ApiResponse(responseCode = "200", description = "Échéance annulée")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentScheduleResponse> cancelSchedule(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        PaymentSchedule cancelled = paymentScheduleService.cancelSchedule(id, schoolId);
        return ResponseEntity.ok(PaymentScheduleResponse.fromEntity(cancelled));
    }
}
