package br.com.betai.service;

import br.com.betai.domain.AnalysisData;
import br.com.betai.domain.Fixture;
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

import java.util.List;
import java.util.Map;

@Service
public class GeminiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAnalysisService.class);
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent";

    private final RestTemplate restTemplate;
    private final DynamoDBService dynamoDBService;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiAnalysisService(RestTemplate restTemplate, DynamoDBService dynamoDBService) {
        this.restTemplate = restTemplate;
        this.dynamoDBService = dynamoDBService;
        this.objectMapper = new ObjectMapper();
    }

    public String analyzeWithContext(Fixture fixture, String statistics, String predictions) {
        AnalysisData analysis = analyzeWithContextDetailed(fixture, statistics, predictions);
        if (analysis == null) {
            return "⚠️ Não foi possível obter análise detalhada do Gemini";
        }

        // Só retorna formatado se o EV for positivo
        if ("POSITIVE".equals(analysis.getBetSuggestion().getStatusEv())
                && analysis.getBetSuggestion().getOddBookmaker() > 0) {
            log.info("Análise com valor encontrada para partida {}. Retornando resultado...", fixture.getId());
            return AnalysisUtils.formatAnalysisToText(analysis, fixture);
        } else {
            log.info("Análise da partida {} descartada para exibição por falta de EV positivo (Calculado: {})",
                    fixture.getId(), String.format("%.2f", analysis.getBetSuggestion().getExpectedValue()));
            return "⚪ Análise descartada: Valor esperado (EV) negativo.";
        }
    }

    public AnalysisData analyzeWithContextDetailed(Fixture fixture, String statistics, String predictions) {
        if ("YOUR_GEMINI_API_KEY".equals(apiKey) || apiKey == null || apiKey.isEmpty()) {
            log.warn("API Key do Gemini não configurada");
            return null;
        }

        var prompt = AnalysisUtils.buildDetailedAnalysisPrompt(fixture, statistics, predictions);
        var url = GEMINI_API_URL + "?key=" + apiKey;

        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of("contents",
                    List.of(Map.of("parts", List.of(Map.of("text", prompt)))), "tools",
                    List.of(Map.of("google_search", Map.of())));

            var request = new HttpEntity<Map<String, Object>>(requestBody, headers);
            var response = restTemplate.postForObject(url, request, GeminiResponse.class);

            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                var aiResponseRaw = extractTextFromResponse(response);
                var analysis = AnalysisUtils.processAnalysisData(aiResponseRaw, objectMapper);

                // Preencher campos extras para o DynamoDB (winner e win_or_draw)
                String market = analysis.getBetSuggestion().getMarket();
                boolean winOrDraw = market != null && (market.toLowerCase().contains("empate")
                        || market.toLowerCase().contains("draw") || market.toLowerCase().contains("chance")
                        || market.toLowerCase().contains("ou x"));
                analysis.setWinOrDraw(winOrDraw);

                if (market != null) {
                    String lowerMarket = market.toLowerCase();
                    String lowerHome = fixture.getHomeTeam().toLowerCase();
                    String lowerAway = fixture.getAwayTeam().toLowerCase();

                    if (lowerMarket.contains(lowerHome)) {
                        analysis.setWinner(new AnalysisData.WinnerNode(fixture.getHomeTeamId(), fixture.getHomeTeam()));
                    } else if (lowerMarket.contains(lowerAway)) {
                        analysis.setWinner(new AnalysisData.WinnerNode(fixture.getAwayTeamId(), fixture.getAwayTeam()));
                    } else if (lowerMarket.startsWith("vitoria casa") || lowerMarket.startsWith("1") || lowerMarket.contains("mandante")) {
                        analysis.setWinner(new AnalysisData.WinnerNode(fixture.getHomeTeamId(), fixture.getHomeTeam()));
                    } else if (lowerMarket.startsWith("vitoria fora") || lowerMarket.startsWith("2") || lowerMarket.contains("visitante")) {
                        analysis.setWinner(new AnalysisData.WinnerNode(fixture.getAwayTeamId(), fixture.getAwayTeam()));
                    }
                }

                dynamoDBService.updateIAAnalysis(fixture.getId(), analysis);
                return analysis;
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API for detailed analysis", e);
        }
        return null;
    }

    private String extractTextFromResponse(GeminiResponse response) {
        return response.candidates().get(0).content().parts().get(0).text();
    }

    // DTOs for Gemini API
    record GeminiResponse(List<Candidate> candidates) {
    }

    record Candidate(Content content) {
    }

    record Content(List<Part> parts) {
    }

    record Part(String text) {
    }
}
