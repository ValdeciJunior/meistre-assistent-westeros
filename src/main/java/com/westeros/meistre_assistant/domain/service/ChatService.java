package com.westeros.meistre_assistant.domain.service;

import com.westeros.meistre_assistant.api.dto.ChatResult;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    public ChatService(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public ChatResult askMeistre(String question, String sourceName) {

        // 1. Configura a busca
        SearchRequest searchRequest = SearchRequest.query(question)
                .withTopK(4)
                .withSimilarityThreshold(0.5);

        // 2. Aplica o filtro de metadados se o livro for especificado
        if (sourceName != null && !sourceName.isBlank()) {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            searchRequest = searchRequest.withFilterExpression(
                    b.eq("source_name", sourceName).build()
            );
        }

        // 3. Executa a busca vetorial no Postgres
        List<Document> similarDocuments = this.vectorStore.similaritySearch(searchRequest);

        // Extrai a lista de trechos reais que o banco encontrou
        List<String> references = similarDocuments.stream()
                .map(Document::getContent)
                .toList();

        // 4. Agrupa os textos dos blocos encontrados para injetar no prompt
        String context = String.join("\n\n", references);

        // 5. Persona do Meistre
        String systemPromptCode = """
                Você é o Meistre de Westeros, um sábio conselheiro especialista nas Crônicas de Gelo e Fogo.
                Use estritamente as informações fornecidas no CONTEXTO abaixo para responder à pergunta do usuário.
                O contexto pode conter trechos de diferentes livros ou mídias integradas.
                Se a resposta não puder ser encontrada ou deduzida do contexto, diga educadamente que seus pergaminhos não contêm essa informação.
                
                CONTEXTO:
                {context}
                """;

        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(systemPromptCode);
        var systemMessage = promptTemplate.createMessage(Map.of("context", context));
        var userMessage = new org.springframework.ai.chat.messages.UserMessage(question);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        // 6. Envia para o Llama 3
        String aiResponse = this.chatModel.call(prompt).getResult().getOutput().getContent();

        // Retorna o resultado envelopado com a resposta da IA e os trechos de referência
        return new ChatResult(aiResponse, references);
    }
}