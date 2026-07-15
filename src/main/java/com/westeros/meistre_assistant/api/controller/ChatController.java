package com.westeros.meistre_assistant.api.controller;

import com.westeros.meistre_assistant.api.dto.ChatResult;
import com.westeros.meistre_assistant.domain.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        String sourceName = payload.get("sourceName");

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "A pergunta não pode estar vazia."));
        }

        // Executa o serviço de chat que agora retorna a resposta e as referências
        ChatResult result = this.chatService.askMeistre(question, sourceName);

        // Retorna o JSON estruturado com ambos os campos
        return ResponseEntity.ok(Map.of(
                "response", result.response(),
                "reference", result.references()
        ));
    }
}