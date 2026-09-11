package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.AccountImportPreviewDTO;
import com.veritech.BudgetKing.service.AccountExportService;
import com.veritech.BudgetKing.service.AccountImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * CSV bulk import/export endpoints for accounts.
 *
 * <p>Import is a two-step flow: the client first uploads the file to {@code /import/preview} to
 * see which rows are valid, duplicated or broken, and only then confirms the import by calling
 * {@code /import/commit} with the same file.</p>
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountImportExportController {

    private final AccountImportService importService;
    private final AccountExportService exportService;

    /**
     * Parses and validates an uploaded CSV file without persisting anything.
     */
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AccountImportPreviewDTO> previewImport(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.preview(file));
    }

    /**
     * Re-validates the uploaded CSV file and persists every valid, non-duplicate row as an
     * account.
     */
    @PostMapping(value = "/import/commit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AccountImportPreviewDTO> commitImport(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.commit(file));
    }

    /**
     * Downloads the current user's accounts as a CSV file.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAccounts() {
        byte[] csv = exportService.exportToCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"accounts.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
