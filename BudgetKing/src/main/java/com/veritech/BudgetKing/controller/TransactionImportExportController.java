package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.ImportPreviewDTO;
import com.veritech.BudgetKing.filter.TransactionFilter;
import com.veritech.BudgetKing.service.TransactionExportService;
import com.veritech.BudgetKing.service.TransactionImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * CSV bulk import/export endpoints for transactions.
 *
 * <p>Import is a two-step flow: the client first uploads the file to {@code /import/preview} to
 * see which rows are valid, duplicated or broken, and only then confirms the import by calling
 * {@code /import/commit} with the same file plus the account the transactions should be created
 * in.</p>
 */
@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionImportExportController {

    private final TransactionImportService importService;
    private final TransactionExportService exportService;

    /**
     * Parses and validates an uploaded CSV file without persisting anything.
     */
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportPreviewDTO> previewImport(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.preview(file));
    }

    /**
     * Re-validates the uploaded CSV file and persists every valid, non-duplicate row as a
     * transaction of the given account.
     */
    @PostMapping(value = "/import/commit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportPreviewDTO> commitImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("accountId") UUID accountId
    ) {
        return ResponseEntity.ok(importService.commit(file, accountId));
    }

    /**
     * Downloads the current user's transactions as a CSV file, optionally filtered.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTransactions(TransactionFilter filter) {
        byte[] csv = exportService.exportToCsv(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
