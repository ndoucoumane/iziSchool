package com.izischool.dashboard.controller;

import com.izischool.auth.service.CurrentUserContextService;
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
import com.izischool.dashboard.service.DirectorDashboardService;
import com.izischool.finance.domain.FeeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/director/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
@Tag(name = "Director Dashboard", description = "Tableau de bord Directeur - Effectifs, Inscriptions, Scolarités, Échéances et Recouvrement")
@SecurityRequirement(name = "BearerAuth")
public class DirectorDashboardController {

    private final DirectorDashboardService directorDashboardService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Synthèse globale du Dashboard Directeur", description = "Retourne l'ensemble des indicateurs tout-en-un (effectifs, finances, mensualités, classes, catégories de frais, méthodes de paiement, impayés, échéances)")
    @ApiResponse(responseCode = "200", description = "Dashboard complet")
    @GetMapping
    public ResponseEntity<DirectorDashboardResponse> getDirectorDashboard(
            @RequestParam(required = false) UUID academicYearId) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        DirectorDashboardResponse response = directorDashboardService.getDirectorDashboard(schoolId, academicYearId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Statistiques globales des effectifs", description = "Effectifs globaux, capacité totale, places libres, taux de remplissage et préinscriptions")
    @ApiResponse(responseCode = "200", description = "Statistiques des effectifs")
    @GetMapping("/enrollment")
    public ResponseEntity<GlobalEnrollmentStatsResponse> getGlobalEnrollment(
            @RequestParam(required = false) UUID academicYearId) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        GlobalEnrollmentStatsResponse response = directorDashboardService.getGlobalEnrollmentStats(schoolId, academicYearId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Situation détaillée de toutes les classes", description = "Effectifs, places disponibles, taux de remplissage et statut financier par classe")
    @ApiResponse(responseCode = "200", description = "Liste des classes avec leurs métriques")
    @GetMapping("/classes")
    public ResponseEntity<List<ClassEnrollmentStatsResponse>> getClassesStats(
            @RequestParam(required = false) UUID academicYearId) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<ClassEnrollmentStatsResponse> response = directorDashboardService.getClassesEnrollmentStats(schoolId, academicYearId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Situation détaillée d'une classe spécifique", description = "Métriques d'effectifs et financières détaillées pour une classe")
    @ApiResponse(responseCode = "200", description = "Détails de la classe")
    @GetMapping("/classes/{classId}")
    public ResponseEntity<ClassEnrollmentStatsResponse> getClassStats(
            @PathVariable UUID classId) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        ClassEnrollmentStatsResponse response = directorDashboardService.getClassEnrollmentStats(schoolId, classId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Synthèse financière globale & recouvrement", description = "Total attendu, encaissé, reste, retards, taux de recouvrement et compteurs d'élèves par statut")
    @ApiResponse(responseCode = "200", description = "Synthèse financière")
    @GetMapping("/payments/summary")
    public ResponseEntity<DirectorPaymentSummaryResponse> getPaymentSummary(
            @RequestParam(required = false) UUID academicYearId) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        DirectorPaymentSummaryResponse response = directorDashboardService.getPaymentSummary(schoolId, academicYearId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Situation dédiée aux frais d'inscription", description = "Montants et effectifs relatifs exclusivement aux frais d'inscription")
    @ApiResponse(responseCode = "200", description = "Synthèse des frais d'inscription")
    @GetMapping("/registrations")
    public ResponseEntity<FeeTypeSummaryResponse> getRegistrationSummary(
            @RequestParam(required = false) UUID academicYearId) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        FeeTypeSummaryResponse response = directorDashboardService.getFeeTypeSummary(schoolId, academicYearId, FeeType.REGISTRATION);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Situation dédiée aux frais de scolarité", description = "Montants et effectifs relatifs aux mensualités et frais de scolarité")
    @ApiResponse(responseCode = "200", description = "Synthèse des frais de scolarité")
    @GetMapping("/tuition")
    public ResponseEntity<FeeTypeSummaryResponse> getTuitionSummary(
            @RequestParam(required = false) UUID academicYearId) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        FeeTypeSummaryResponse response = directorDashboardService.getFeeTypeSummary(schoolId, academicYearId, FeeType.TUITION);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Suivi des mensualités / échéances du mois", description = "Situation détaillée d'un mois (attendu, encaissé, reste, retards, compteurs d'échéances)")
    @ApiResponse(responseCode = "200", description = "Situation des mensualités")
    @GetMapping("/schedules/monthly")
    public ResponseEntity<MonthlyScheduleStatusResponse> getMonthlyScheduleStatus(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        MonthlyScheduleStatusResponse response = directorDashboardService.getMonthlyScheduleStatus(schoolId, academicYearId, month, year);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Situation financière individuelle par élève", description = "Liste paginée de la situation élève par élève avec filtres par classe, statut (UP_TO_DATE, PARTIALLY_PAID, UNPAID, OVERDUE) et recherche")
    @ApiResponse(responseCode = "200", description = "Page des statuts financiers élèves")
    @GetMapping("/students/financial-status")
    public ResponseEntity<PageResponse<StudentFinancialStatusResponse>> getStudentsFinancialStatus(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<StudentFinancialStatusResponse> response = directorDashboardService.getStudentsFinancialStatus(
                schoolId, academicYearId, classId, status, search, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Répartition par catégorie de frais", description = "Montants attendus, collectés et pourcentages par type et intitulé de frais")
    @ApiResponse(responseCode = "200", description = "Répartition par catégorie de frais")
    @GetMapping("/payments/by-category")
    public ResponseEntity<List<FeeCategoryBreakdownResponse>> getFeeCategoryBreakdown(
            @RequestParam(required = false) UUID academicYearId) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<FeeCategoryBreakdownResponse> response = directorDashboardService.getFeeCategoryBreakdown(schoolId, academicYearId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Répartition par moyen de paiement", description = "Répartition des encaissements par méthode (Wave, Orange Money, Espèces, etc.)")
    @ApiResponse(responseCode = "200", description = "Répartition par moyen de paiement")
    @GetMapping("/payments/by-method")
    public ResponseEntity<List<PaymentMethodBreakdownResponse>> getPaymentMethodBreakdown(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<PaymentMethodBreakdownResponse> response = directorDashboardService.getPaymentMethodBreakdown(
                schoolId, academicYearId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Évolution chronologique des encaissements", description = "Évolution des paiements regroupés par jour, semaine ou mois (DAY, WEEK, MONTH)")
    @ApiResponse(responseCode = "200", description = "Série temporelle des encaissements")
    @GetMapping("/payments/evolution")
    public ResponseEntity<List<PaymentEvolutionResponse>> getPaymentEvolution(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(defaultValue = "MONTH") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<PaymentEvolutionResponse> response = directorDashboardService.getPaymentEvolution(
                schoolId, academicYearId, groupBy, dateFrom, dateTo);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Suivi des impayés et retards", description = "Liste paginée des élèves en retard avec montant impayé, ancienneté (0-7j, 8-30j, 30j+) et contact parent financier")
    @ApiResponse(responseCode = "200", description = "Page des élèves en retard")
    @GetMapping("/overdue")
    public ResponseEntity<PageResponse<OverdueStudentPaymentResponse>> getOverdueStudents(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OverdueStudentPaymentResponse> response = directorDashboardService.getOverdueStudents(
                schoolId, academicYearId, classId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Échéancier prévisionnel des paiements à venir", description = "Liste des échéances à venir sur les N prochains jours avec montants et compteurs d'élèves")
    @ApiResponse(responseCode = "200", description = "Échéances prévisionnelles")
    @GetMapping("/upcoming-due-dates")
    public ResponseEntity<List<UpcomingDueDateResponse>> getUpcomingDueDates(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(defaultValue = "30") int daysAhead) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<UpcomingDueDateResponse> response = directorDashboardService.getUpcomingDueDates(
                schoolId, academicYearId, daysAhead);
        return ResponseEntity.ok(response);
    }
}
