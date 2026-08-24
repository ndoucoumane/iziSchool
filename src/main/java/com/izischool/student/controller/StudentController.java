package com.izischool.student.controller;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.service.AcademicYearService;
import com.izischool.academic.service.SchoolClassService;
import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.exception.BusinessException;
import com.izischool.common.response.PageResponse;
import com.izischool.common.util.MoneyUtils;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeType;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.dto.PaymentScheduleResponse;
import com.izischool.finance.repository.FeeRepository;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.finance.service.FeeService;
import com.izischool.finance.service.PaymentScheduleService;
import com.izischool.parent.domain.Parent;
import com.izischool.parent.domain.ParentRelationship;
import com.izischool.parent.service.ParentService;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.dto.PaymentResponse;
import com.izischool.payment.dto.ReceiptResponse;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.payment.repository.ReceiptRepository;
import com.izischool.payment.service.PaymentService;
import com.izischool.school.domain.School;
import com.izischool.student.domain.EnrollmentStatus;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentEnrollment;
import com.izischool.student.domain.StudentStatus;
import com.izischool.student.dto.StudentBalanceResponse;
import com.izischool.student.dto.StudentRequest;
import com.izischool.student.dto.StudentResponse;
import com.izischool.student.dto.UpdateStudentStatusRequest;
import com.izischool.student.repository.StudentEnrollmentRepository;
import com.izischool.student.service.StudentEnrollmentService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Gestion des élèves, inscriptions, soldes financiers, échéanciers et historiques")
@SecurityRequirement(name = "BearerAuth")
public class StudentController {

    private final StudentService studentService;
    private final StudentEnrollmentService studentEnrollmentService;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final SchoolClassService schoolClassService;
    private final AcademicYearService academicYearService;
    private final ParentService parentService;
    private final FeeService feeService;
    private final FeeRepository feeRepository;
    private final PaymentScheduleService paymentScheduleService;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Lister les élèves", description = "Retourne la liste paginée des élèves avec filtres (recherche, statut) et statuts financiers")
    @ApiResponse(responseCode = "200", description = "Liste paginée des élèves")
    @GetMapping
    public ResponseEntity<PageResponse<StudentResponse>> listStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Page<Student> studentPage = studentService.getStudentsPaged(schoolId, search, status, PageRequest.of(page, size, Sort.by("lastName").ascending()));
        return ResponseEntity.ok(PageResponse.of(studentPage, s -> enrichStudentResponse(s, schoolId)));
    }

    @Operation(summary = "Créer un élève", description = "Enregistre un nouvel élève, avec inscription en classe, génération automatique de la scolarité et encaissement du versement initial optionnel")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Élève créé et inscrit avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        School school = currentUserContextService.getCurrentSchool()
                .orElseThrow(() -> new IllegalStateException("School context required"));

        Student student = request.toEntity(school);
        Student createdStudent = studentService.createStudent(student);

        UUID classId = null;
        String className = null;

        // Inscription en classe optionnelle
        if (request.getClassId() != null) {
            SchoolClass schoolClass = schoolClassService.getSchoolClassById(request.getClassId(), schoolId);
            classId = schoolClass.getId();
            className = schoolClass.getName();

            AcademicYear activeYear = academicYearService.getActiveAcademicYear(schoolId)
                    .orElseThrow(() -> new BusinessException("No active academic year found for school"));

            StudentEnrollment enrollment = StudentEnrollment.builder()
                    .school(school)
                    .student(createdStudent)
                    .academicYear(activeYear)
                    .schoolClass(schoolClass)
                    .enrollmentDate(LocalDate.now())
                    .status(EnrollmentStatus.ACTIVE)
                    .build();
            StudentEnrollment savedEnrollment = studentEnrollmentService.enrollStudent(enrollment);

            // Gestion de la scolarité et du versement initial lors de l'inscription
            if (request.getTotalDue() != null && MoneyUtils.isPositive(request.getTotalDue())) {
                Fee fee;
                if (request.getFeeId() != null) {
                    fee = feeService.getFeeById(request.getFeeId(), schoolId);
                } else {
                    List<Fee> tuitionFees = feeRepository.findBySchool_IdAndAcademicYear_IdAndFeeTypeAndActiveTrue(
                            schoolId, activeYear.getId(), FeeType.TUITION);
                    if (!tuitionFees.isEmpty()) {
                        fee = tuitionFees.get(0);
                    } else {
                        fee = feeRepository.save(Fee.builder()
                                .school(school)
                                .academicYear(activeYear)
                                .name("Frais de scolarité - " + schoolClass.getName())
                                .code("SCOL-" + schoolClass.getCode() + "-" + (System.currentTimeMillis() % 10000))
                                .amount(MoneyUtils.scale(request.getTotalDue()))
                                .currency(school.getCurrency() != null ? school.getCurrency() : "XOF")
                                .feeType(FeeType.TUITION)
                                .active(true)
                                .mandatory(true)
                                .build());
                    }
                }

                int installments = (request.getNumberOfInstallments() != null && request.getNumberOfInstallments() > 0)
                        ? request.getNumberOfInstallments()
                        : 1;

                List<PaymentSchedule> schedules = paymentScheduleService.generateSchedulesDirect(
                        createdStudent,
                        savedEnrollment,
                        fee,
                        installments,
                        request.getTotalDue(),
                        LocalDate.now()
                );

                // Traitement du versement initial lors de l'inscription
                if (request.getInitialPayment() != null && MoneyUtils.isPositive(request.getInitialPayment())) {
                    PaymentMethod method = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CASH;
                    Payment initialPayment = Payment.builder()
                            .school(school)
                            .student(createdStudent)
                            .amount(MoneyUtils.scale(request.getInitialPayment()))
                            .currency(school.getCurrency() != null ? school.getCurrency() : "XOF")
                            .paymentMethod(method)
                            .provider(method == PaymentMethod.MOBILE_MONEY ? PaymentProvider.WAVE : PaymentProvider.MANUAL)
                            .description(String.format("Versement initial lors de l'inscription en %s", schoolClass.getName()))
                            .build();

                    paymentService.processPayment(initialPayment);
                }
            }
        }

        // Lien parent optionnel
        if (request.getParentId() != null) {
            Parent parent = parentService.getParentById(request.getParentId(), schoolId);
            parentService.linkStudentAndParent(createdStudent, parent, ParentRelationship.FATHER, true, true);
        }

        StudentResponse response = enrichStudentResponse(createdStudent, schoolId);
        if (response.getClassId() == null && classId != null) {
            response.setClassId(classId);
            response.setClassName(className);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Détails d'un élève", description = "Récupère la fiche détaillée d'un élève avec son solde et sa classe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Élève trouvé"),
            @ApiResponse(responseCode = "404", description = "Élève non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Student student = studentService.getStudentById(id, schoolId);
        return ResponseEntity.ok(enrichStudentResponse(student, schoolId));
    }

    @Operation(summary = "Modifier un élève", description = "Met à jour les informations d'un élève")
    @ApiResponse(responseCode = "200", description = "Élève mis à jour")
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable UUID id,
            @Valid @RequestBody StudentRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Student studentData = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .dateOfBirth(request.getDateOfBirth())
                .placeOfBirth(request.getPlaceOfBirth())
                .gender(request.getGender())
                .photoUrl(request.getPhotoUrl())
                .build();

        Student updated = studentService.updateStudent(id, schoolId, studentData);
        return ResponseEntity.ok(enrichStudentResponse(updated, schoolId));
    }

    @Operation(summary = "Changer le statut d'un élève", description = "Met à jour le statut d'un élève (ACTIVE, SUSPENDED, TRANSFERRED, GRADUATED, DROPPED_OUT)")
    @ApiResponse(responseCode = "200", description = "Statut mis à jour")
    @PatchMapping("/{id}/status")
    public ResponseEntity<StudentResponse> updateStudentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudentStatusRequest request) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Student updated = studentService.updateStudentStatus(id, schoolId, request.getStatus());
        return ResponseEntity.ok(enrichStudentResponse(updated, schoolId));
    }

    @Operation(summary = "Supprimer un élève", description = "Supprime / désactive logiquement un élève (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Élève supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Élève non trouvé")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteStudent(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        studentService.deleteStudent(id, schoolId);
        return ResponseEntity.ok(Map.of("message", "Student deleted successfully"));
    }

    @Operation(summary = "Solde financier d'un élève", description = "Calcule et retourne le total attendu, total payé et reste à payer d'un élève")
    @ApiResponse(responseCode = "200", description = "Solde calculé")
    @GetMapping("/{id}/balance")
    public ResponseEntity<StudentBalanceResponse> getStudentBalance(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        StudentBalanceResponse balance = studentService.getStudentBalance(id, schoolId);
        return ResponseEntity.ok(balance);
    }

    @Operation(summary = "Échéanciers d'un élève", description = "Retourne la liste ordonnée des échéances de paiement d'un élève")
    @ApiResponse(responseCode = "200", description = "Liste des échéances")
    @GetMapping("/{id}/schedules")
    public ResponseEntity<List<PaymentScheduleResponse>> getStudentSchedules(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<PaymentSchedule> schedules = paymentScheduleService.getSchedulesByStudent(schoolId, id);
        List<PaymentScheduleResponse> response = schedules.stream()
                .map(PaymentScheduleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Historique des paiements d'un élève", description = "Retourne la liste des règlements effectués pour un élève")
    @ApiResponse(responseCode = "200", description = "Historique des paiements")
    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PaymentResponse>> getStudentPayments(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<Payment> payments = paymentRepository.findBySchool_IdAndStudent_IdAndDeletedFalseOrderByPaymentDateDesc(schoolId, id);
        List<PaymentResponse> response = payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reçus d'un élève", description = "Retourne l'ensemble des reçus générés pour un élève")
    @ApiResponse(responseCode = "200", description = "Liste des reçus de l'élève")
    @GetMapping("/{id}/receipts")
    public ResponseEntity<List<ReceiptResponse>> getStudentReceipts(@PathVariable UUID id) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<Payment> payments = paymentRepository.findBySchool_IdAndStudent_IdAndDeletedFalseOrderByPaymentDateDesc(schoolId, id);
        List<ReceiptResponse> receipts = payments.stream()
                .map(p -> receiptRepository.findByPayment_Id(p.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ReceiptResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(receipts);
    }

    // ---------------------------------------------------------------------------------------------
    // Helper: Enrich StudentResponse with Class & Financial Status
    // ---------------------------------------------------------------------------------------------
    private StudentResponse enrichStudentResponse(Student student, UUID schoolId) {
        if (student == null) {
            return null;
        }

        UUID classId = null;
        String className = null;

        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findBySchool_Id(schoolId);
        Optional<StudentEnrollment> enrollmentOpt = enrollments.stream()
                .filter(e -> e.getStudent() != null && e.getStudent().getId().equals(student.getId()))
                .filter(e -> e.getStatus() != EnrollmentStatus.CANCELLED)
                .findFirst();

        if (enrollmentOpt.isPresent()) {
            StudentEnrollment enrollment = enrollmentOpt.get();
            if (enrollment.getSchoolClass() != null) {
                classId = enrollment.getSchoolClass().getId();
                className = enrollment.getSchoolClass().getName();
            }
        }

        BigDecimal totalDue = paymentScheduleRepository.sumStudentTotalExpectedAmount(schoolId, student.getId());
        BigDecimal totalPaid = paymentScheduleRepository.sumStudentTotalCollectedAmount(schoolId, student.getId());
        BigDecimal remainingAmount = paymentScheduleRepository.sumStudentTotalOutstandingAmount(schoolId, student.getId());

        String paymentStatus = "PENDING";
        if (MoneyUtils.isPositive(totalDue)) {
            if (MoneyUtils.isZero(remainingAmount)) {
                paymentStatus = "PAID";
            } else if (MoneyUtils.isPositive(totalPaid)) {
                paymentStatus = "PARTIALLY_PAID";
            } else {
                paymentStatus = "UNPAID";
            }
        }

        return StudentResponse.fromEntity(
                student, classId, className, totalDue, totalPaid, remainingAmount, paymentStatus);
    }
}
