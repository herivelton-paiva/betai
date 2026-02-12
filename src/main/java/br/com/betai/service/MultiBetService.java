package br.com.betai.service;

import br.com.betai.domain.Fixture;
import br.com.betai.domain.MultiBetResponse;
import br.com.betai.utils.AnalysisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MultiBetService {

    private static final Logger log = LoggerFactory.getLogger(MultiBetService.class);
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent";

    private final DynamoDBService dynamoDBService;
    private final RestTemplate restTemplate;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    public MultiBetService(DynamoDBService dynamoDBService, RestTemplate restTemplate,
            NotificationService notificationService) {
        this.dynamoDBService = dynamoDBService;
        this.restTemplate = restTemplate;
        this.notificationService = notificationService;
        this.objectMapper = new ObjectMapper();
    }

    public void generateAndSendMultiples() {
        LocalDate today = LocalDate.now();
        log.info("Iniciando geração de múltiplas para o dia {}", today);

        List<Fixture> fixtures = dynamoDBService.getFixturesByDateMapped(today);

        // Filtrar partidas com status NS e que possuam iaAnalysis
        List<Fixture> analyzedFixtures = fixtures.stream().filter(f -> "NS".equals(f.getStatusShort()))
                .filter(f -> f.getIaAnalysis() != null).collect(Collectors.toList());

        if (analyzedFixtures.size() < 4) {
            log.warn("Quantidade insuficiente de jogos analisados para gerar múltiplas (Mínimo: 4, Total: {})",
                    analyzedFixtures.size());
            return;
        }

        String prompt = AnalysisUtils.buildMultiBetPrompt(analyzedFixtures);

        try {
            MultiBetResponse response = callGeminiForMultiples(prompt);

            if (response != null && response.getMultiples() != null) {
                int count = 1;
                for (MultiBetResponse.MultipleSuggestion suggestion : response.getMultiples()) {
                    String formattedMessage = AnalysisUtils.formatMultipleToText(suggestion);
                    log.info("Sugestão de Múltipla {}:\n{}", count, formattedMessage);

                    // Enviar para o Telegram se configurado
                    notificationService.sendToTelegram(formattedMessage, count, response.getMultiples().size());
                    count++;
                }
            } else {
                log.warn("Nenhuma múltipla gerada pelo Gemini.");
            }
        } catch (Exception e) {
            log.error("Erro ao gerar múltiplas via Gemini", e);
        }
    }

    @SuppressWarnings("unchecked")
    private MultiBetResponse callGeminiForMultiples(String prompt) {
        if ("YOUR_GEMINI_API_KEY".equals(apiKey) || apiKey == null || apiKey.isEmpty()) {
            log.error("API Key do Gemini não configurada para múltiplas");
            return null;
        }

        var url = GEMINI_API_URL + "?key=" + apiKey;
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("response_mime_type", "application/json"));

        var request = new HttpEntity<>(requestBody, headers);

        try {
            var response = restTemplate.postForObject(url, request, Map.class);
            if (response != null) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    String aiText = (String) parts.get(0).get("text");

                    return AnalysisUtils.processMultiBetData(aiText, objectMapper);
                }
            }
        } catch (Exception e) {
            log.error("Erro na chamada da API Gemini para múltiplas", e);
        }
        return null;
    }
}
