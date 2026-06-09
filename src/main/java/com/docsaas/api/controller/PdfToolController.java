package com.docsaas.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.dto.CompressPdfRequestDTO;
import com.docsaas.api.dto.ConvertPdfRequestDTO;
import com.docsaas.api.dto.EditPdfRequestDTO;
import com.docsaas.api.dto.JobResponseDTO;
import com.docsaas.api.dto.MergePdfRequestDTO;
import com.docsaas.api.dto.OcrRequestDTO;
import com.docsaas.api.dto.PdfSummaryRequestDTO;
import com.docsaas.api.dto.ProtectPdfRequestDTO;
import com.docsaas.api.dto.SplitPdfRequestDTO;
import com.docsaas.api.dto.UnlockPdfRequestDTO;
import com.docsaas.api.service.JobService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pdf")
@RequiredArgsConstructor
public class PdfToolController {

    private final JobService jobService;

    @PostMapping("/merge")
    public ResponseEntity<JobResponseDTO> merge(@RequestBody MergePdfRequestDTO request) {
        return ResponseEntity.ok(jobService.createJob("MERGE", request.getFileIds().get(0)));
    }

    @PostMapping("/split")
    public ResponseEntity<JobResponseDTO> split(@RequestBody SplitPdfRequestDTO request) {
        return ResponseEntity.ok(jobService.createJob("SPLIT", request.getFileId()));
    }

    @PostMapping("/compress")
    public ResponseEntity<JobResponseDTO> compress(@RequestBody CompressPdfRequestDTO request) {
        return ResponseEntity.ok(jobService.createJob("COMPRESS", request.getFileId()));
    }

    @PostMapping("/convert")
    public ResponseEntity<JobResponseDTO> convert(@RequestBody ConvertPdfRequestDTO request) {
        return ResponseEntity.ok(jobService.createJob("CONVERT", request.getFileId()));
    }

    @PostMapping("/protect")
    public ResponseEntity<JobResponseDTO> protect(@RequestBody ProtectPdfRequestDTO request) {
        return ResponseEntity.ok(jobService.createJob("PROTECT_PDF", request.getFileId()));
    }

    @PostMapping("/unlock")
    public ResponseEntity<JobResponseDTO> unlock(@RequestBody UnlockPdfRequestDTO request) {
        return ResponseEntity.ok(jobService.createJob("UNLOCK_PDF", request.getFileId()));
    }

    @PostMapping("/edit")
    public ResponseEntity<JobResponseDTO> edit(@RequestBody EditPdfRequestDTO request) {
        return ResponseEntity.ok(jobService.createJob("EDIT_TEXT", request.getFileId()));
    }

    @PostMapping("/ocr")
    public ResponseEntity<JobResponseDTO> ocr(@RequestBody OcrRequestDTO request) {
        return ResponseEntity.ok(jobService.createJob("OCR_EXTRACT", request.getFileId()));
    }

    @PostMapping("/summarize")
    public ResponseEntity<JobResponseDTO> summarize(@RequestBody PdfSummaryRequestDTO request) {
        return ResponseEntity.ok(jobService.createJob("PDF_SUMMARY", request.getFileId()));
    }
}
