package com.westeros.meistre_assistant.api.controller;

import com.westeros.meistre_assistant.domain.service.model.MediaType;
import com.westeros.meistre_assistant.domain.service.IngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/pdf")
    public ResponseEntity<String> uploadKnowledgeBase(
            @RequestParam("file") MultipartFile file,
            @RequestParam("source") String source,
            @RequestParam("mediaType") MediaType mediaType) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, envie um arquivo válido.");
        }

        ingestionService.ingestPdf(file, source, mediaType);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Conhecimento transmídia guardado com sucesso! Fonte: " + source + " [" + mediaType + "]");
    }
}