package com.westeros.meistre_assistant.domain.service;

import com.westeros.meistre_assistant.domain.service.model.MediaType;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class IngestionService {

    private final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestPdf(MultipartFile file, String sourceName, MediaType mediaType) {
        try {
            InputStreamResource pdfResource = new InputStreamResource(file.getInputStream());
            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder().build();
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource, config);
            List<Document> rawDocuments = pdfReader.get();

            // Identifica automaticamente o Cânone com base no tipo de mídia
            String canon = (mediaType == MediaType.TV_SHOW || mediaType == MediaType.MOVIE)
                    ? "SHOW_CANON"
                    : "BOOK_CANON";

            // CRIA UMA NOVA LISTA DE DOCUMENTOS:
            // Evita tentar alterar os documentos originais que são imutáveis.
            List<Document> cleanedDocuments = rawDocuments.stream()
                    .map(doc -> {
                        // 1. Captura e limpa os metadados existentes (evitando o NullPointerException anterior)
                        var metadata = doc.getMetadata();
                        if (metadata != null) {
                            metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
                        }

                        // 2. Injeta as suas tags customizadas no mapa de metadados
                        metadata.put("source_name", sourceName);
                        metadata.put("media_type", mediaType.name());
                        metadata.put("canon", canon);

                        // 3. Limpa o texto (removendo quebras de linha duras e excesso de espaços)
                        String cleanContent = "";
                        if (doc.getContent() != null) {
                            cleanContent = doc.getContent()
                                    .replaceAll("\\r?\\n", " ")  // Substitui quebras de linha por espaço
                                    .replaceAll("\\s{2,}", " ")  // Substitui múltiplos espaços por um espaço único
                                    .trim();
                        }

                        // 4. Retorna uma NOVA instância novinha do Document com o ID, conteúdo limpo e metadados
                        return new Document(doc.getId(), cleanContent, metadata);
                    }).toList();

            // Divisão em blocos de texto (Chunks) usando a lista recém-higienizada
            TokenTextSplitter textSplitter = new TokenTextSplitter(1000, 200, 10, 10000, true);
            List<Document> splitDocuments = textSplitter.apply(cleanedDocuments);

            // Salva no banco vetorial incluindo todas as tags de metadados tratadas
            this.vectorStore.accept(splitDocuments);

        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar os pergaminhos transmídia de Westeros", e);
        }
    }
}