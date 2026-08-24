package com.izischool.payment.controller;

import com.izischool.auth.service.CurrentUserContextService;
import com.izischool.common.response.PageResponse;
import com.izischool.payment.domain.Receipt;
import com.izischool.payment.dto.ReceiptResponse;
import com.izischool.payment.service.PdfReceiptGeneratorService;
import com.izischool.payment.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts", description = "Consultation et téléchargement des reçus de paiement")
@SecurityRequirement(name = "BearerAuth")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final PdfReceiptGeneratorService pdfReceiptGeneratorService;
    private final CurrentUserContextService currentUserContextService;

    @Operation(summary = "Lister les reçus", description = "Retourne la liste paginée de tous les reçus de paiement émis")
    @ApiResponse(responseCode = "200", description = "Liste paginée des reçus")
    @GetMapping
    public ResponseEntity<PageResponse<ReceiptResponse>> listReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Page<Receipt> receiptPage = receiptService.getReceiptsBySchool(schoolId, PageRequest.of(page, size, Sort.by("issuedAt").descending()));
        return ResponseEntity.ok(PageResponse.of(receiptPage, ReceiptResponse::fromEntity));
    }

    @Operation(summary = "Détails d'un reçu", description = "Récupère les informations d'un reçu par son ID (UUID) ou son numéro de reçu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reçu trouvé"),
            @ApiResponse(responseCode = "404", description = "Reçu non trouvé")
    })
    @GetMapping("/{idOrNumber}")
    public ResponseEntity<ReceiptResponse> getReceiptById(@PathVariable String idOrNumber) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Receipt receipt = receiptService.getReceiptByIdOrNumber(idOrNumber, schoolId);
        return ResponseEntity.ok(ReceiptResponse.fromEntity(receipt));
    }

    @Operation(summary = "Télécharger le reçu en PDF", description = "Génère et affiche/télécharge le fichier PDF officiel du reçu")
    @ApiResponse(responseCode = "200", description = "Fichier PDF binaire")
    @GetMapping(value = "/{idOrNumber}/pdf")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable String idOrNumber) {
        UUID schoolId = currentUserContextService.getRequiredSchoolId();
        Receipt receipt = receiptService.getReceiptByIdOrNumber(idOrNumber, schoolId);
        byte[] pdfBytes = pdfReceiptGeneratorService.generateReceiptPdf(receipt);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recu-" + receipt.getReceiptNumber() + ".pdf\"")
                .body(pdfBytes);
    }

    @Operation(summary = "Reçu par ID de paiement", description = "Récupère le reçu associé à un paiement")
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<ReceiptResponse> getReceiptByPaymentId(@PathVariable UUID paymentId) {
        Receipt receipt = receiptService.getReceiptByPaymentId(paymentId);
        return ResponseEntity.ok(ReceiptResponse.fromEntity(receipt));
    }

    @Operation(summary = "Télécharger le PDF d'un reçu par ID de paiement", description = "Génère et affiche le fichier PDF du reçu associé à un paiement")
    @GetMapping("/payment/{paymentId}/pdf")
    public ResponseEntity<byte[]> downloadReceiptPdfByPaymentId(@PathVariable UUID paymentId) {
        Receipt receipt = receiptService.getReceiptByPaymentId(paymentId);
        byte[] pdfBytes = pdfReceiptGeneratorService.generateReceiptPdf(receipt);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recu-" + receipt.getReceiptNumber() + ".pdf\"")
                .body(pdfBytes);
    }
}
