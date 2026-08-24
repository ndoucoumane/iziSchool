package com.izischool.dashboard.controller;

import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.dashboard.dto.ArrearsReportResponse;
import com.izischool.dashboard.dto.DashboardOverviewResponse;
import com.izischool.dashboard.dto.RevenueTrendItem;
import com.izischool.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicateurs financiers, taux de recouvrement, impayés et graphiques de revenus")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Vue d'ensemble du Dashboard", description = "Retourne les KPIs financiers (effectifs, total attendu, collecté, reste à payer, taux de recouvrement, impayés)")
    @ApiResponse(responseCode = "200", description = "KPIs calculés")
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview() {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        DashboardOverviewResponse overview = dashboardService.getOverview(schoolId);
        return ResponseEntity.ok(overview);
    }

    @Operation(summary = "Tendance des revenus", description = "Retourne l'évolution journalière des encaissements sur les N derniers jours")
    @ApiResponse(responseCode = "200", description = "Série temporelle des encaissements")
    @GetMapping("/revenue-trend")
    public ResponseEntity<List<RevenueTrendItem>> getRevenueTrend(@RequestParam(defaultValue = "7") int days) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        List<RevenueTrendItem> trend = dashboardService.getRevenueTrend(schoolId, days);
        return ResponseEntity.ok(trend);
    }

    @Operation(summary = "Rapport des impayés (Arrears)", description = "Retourne la répartition des créances par tranche de retard (0-7j, 8-30j, 30j+)")
    @ApiResponse(responseCode = "200", description = "Rapport d'ancienneté des créances")
    @GetMapping("/arrears")
    public ResponseEntity<ArrearsReportResponse> getArrearsReport() {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        ArrearsReportResponse report = dashboardService.getArrearsReport(schoolId);
        return ResponseEntity.ok(report);
    }
}
