package br.com.betai.service;

import br.com.betai.domain.Fixture;
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
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiAnalysisService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String analyzeWithContext(Fixture fixture, String statistics, String predictions) {
        if ("YOUR_GEMINI_API_KEY".equals(apiKey) || apiKey == null || apiKey.isEmpty()) {
            return "⚠️ API Key do Gemini não configurada";
        }

        String prompt = buildDetailedAnalysisPrompt(fixture, statistics, predictions);
        String url = GEMINI_API_URL + "?key=" + apiKey;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of("contents",
                    List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            GeminiResponse response = restTemplate.postForObject(url, request, GeminiResponse.class);

            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                return extractTextFromResponse(response);
            }

            return "⚠️ Não foi possível obter análise detalhada do Gemini";
        } catch (Exception e) {
            log.error("Error calling Gemini API for detailed analysis", e);
            return "❌ Erro ao analisar com contexto: " + e.getMessage();
        }
    }

    private String buildDetailedAnalysisPrompt(Fixture fixture, String statistics, String predictions) {
        String oddsSection = "";
        if (fixture.getOdds() != null && !fixture.getOdds().isEmpty() && !"{ }".equals(fixture.getOdds())
                && !"{}".equals(fixture.getOdds())) {
            oddsSection = String.format("\n--- 💰 ODDS ATUAIS ---\n%s\n", fixture.getOdds());
        }

        String gameTime = fixture.getDate() != null
                ? fixture.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "--/--/---- --:--";

        return String.format(
                """
                        Com base nos dados abaixo para o confronto %s x %s (%s):
                        %s
                        --- 📊 ESTATÍSTICAS REAIS ---
                        %s

                        --- 🤖 PREVISÕES TÉCNICAS ---
                        %s

                        Forneça APENAS a Sugestão de Aposta seguindo EXATAMENTE o formato abaixo.

                        ⚠️ REGRAS OBRIGATÓRIAS:
                        1. PROIBIDO 0%%: Nunca utilize 0%% de probabilidade para nenhum resultado (Casa, Empate, Fora), pois futebol aceita surpresas.
                        2. ODDS EM TUDO: Sempre que houver ODDS ATUAIS fornecidas, coloque a Odd correspondente entre parênteses (ex: 1.85) imediatamente após cada sugestão, mercado, probabilidade ou placar provável.
                        3. ODD COMBINADA: Se sugerir uma aposta com 2 condições (ex: Time A ou Empate E Menos de 3.5 gols), inclua a linha "Odd Combinada" com o valor total.

                        ### 🎯 SUGESTÃO DE APOSTA (%s x %s - %s)
                        **[Sua sugestão principal aqui (Odd)]**
                        **Odd Combinada:** [Valor se houver 2 condições]

                        **🔥 MERCADO DE GOLS:** [Palpite de Gols (Odd)]
                        **📊 PROBABILIDADES:** Casa: [%%] (Odd) | Empate: [%%] (Odd) | Fora: [%%] (Odd)
                        **⚽ PLACAR PROVÁVEL:** %s [Placar] %s (Odd)
                        **📈 NÍVEL DE CONFIANÇA:** [Baixo/Médio/Alto/Muito Alto]

                        Responda em português. Seja curto e direto.
                        """,
                fixture.getHomeTeam(), fixture.getAwayTeam(), fixture.getLeagueName(), oddsSection, statistics,
                predictions, fixture.getHomeTeam(), fixture.getAwayTeam(), gameTime, fixture.getHomeTeam(),
                fixture.getAwayTeam());
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
