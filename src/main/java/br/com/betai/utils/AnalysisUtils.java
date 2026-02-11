package br.com.betai.utils;

import br.com.betai.domain.AnalysisData;
import br.com.betai.domain.Fixture;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;

public class AnalysisUtils {

    private AnalysisUtils() {
        // Utility class
    }

    public static String buildDetailedAnalysisPrompt(Fixture fixture, String statistics, String predictions) {
        var oddsSection = "";
        if (fixture.getOdds() != null && !fixture.getOdds().isEmpty() && !"{ }".equals(fixture.getOdds())
                && !"{}".equals(fixture.getOdds())) {
            oddsSection = String.format("\n--- 💰 ODDS ATUAIS ---\n%s\n", fixture.getOdds());
        }

        return buildPrompt(fixture.getHomeTeam(), fixture.getAwayTeam(), fixture.getLeagueName(), oddsSection,
                statistics, predictions, fixture.getId(),
                fixture.getDate() != null ? fixture.getDate().toString() : "");
    }

    private static String buildPrompt(String home, String away, String league, String oddsSection, String statistics,
            String predictions, Long fixtureId, String date) {
        return String.format("""
                Você é um Analista de Apostas e Cientista de Dados.
                Sua tarefa: Gerar uma análise técnica em JSON para o jogo %s x %s (%s) em %s.

                --- CONTEXTO E ODDS ---
                %s
                --- ESTATÍSTICAS ---
                %s
                --- MODELOS DE REFERÊNCIA ---
                %s

                REGRAS DE OURO:
                1. INDEPENDÊNCIA: Use o contexto acima, mas pesquise por notícias (Search) e decida por conta própria.
                2. MERCADOS PROIBIDOS: NUNCA sugira 'Handicap Asiático'. Use 1X2, Dupla Chance ou Gols.
                3. FORMATO: Retorne APENAS o JSON puro. NADA de explicações, preâmbulos ou markdown.
                4. IDIOMA: Português técnico (Brasil).

                ESTRUTURA OBRIGATÓRIA (JSON):
                {
                "fixture": { "id": %d, "teams": { "home": "%s", "away": "%s" }, "date": "%s" },
                "bet_suggestion": {
                    "market": "Mercado Selecionado",
                    "odd_bookmaker": 0.00,
                    "probability_ai": 0.00,
                    "justification": "Explicação técnica curta"
                },
                "goals_market": { "target": "Mercado de Gols", "odd": 0.00 },
                "probabilities": { "home_win": 0.00, "draw": 0.00, "away_win": 0.00, "confidence_level": "MEDIO" },
                "prediction": { "correct_score": "X:Y", "score_odd": 0.00 }
                }
                """, home, away, league, date, oddsSection, statistics, predictions, fixtureId, home, away, date);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AnalysisUtils.class);

    public static AnalysisData processAnalysisData(String aiResponseRaw, ObjectMapper objectMapper)
            throws JsonProcessingException {
        if (aiResponseRaw == null || aiResponseRaw.trim().isEmpty()) {
            throw new com.fasterxml.jackson.databind.JsonMappingException(null, "Resposta da IA veio vazia.");
        }

        // Remove markdown code blocks BEFORE seeking braces
        String raw = aiResponseRaw.replace("```json", "").replace("```", "").trim();

        // Extrair JSON da resposta buscando do primeiro { ao último }
        int firstBrace = raw.indexOf('{');
        int lastBrace = raw.lastIndexOf('}');

        if (firstBrace == -1 || lastBrace == -1 || lastBrace < firstBrace) {
            log.error("JSON não encontrado. Resposta completa do Gemini: \n{}", aiResponseRaw);
            throw new com.fasterxml.jackson.databind.JsonMappingException(null,
                    "ERRO: O Gemini não retornou um JSON válido. Verifique os logs para ver a resposta bruta.");
        }

        var jsonStr = raw.substring(firstBrace, lastBrace + 1);

        // Limpeza agressiva de vírgulas pendentes
        jsonStr = jsonStr.replaceAll(",\\s*([}\\]])", "$1");

        AnalysisData analysis;
        try {
            analysis = objectMapper.readValue(jsonStr, AnalysisData.class);
        } catch (Exception e) {
            log.warn("Falha no parse inicial (provavelmente truncado). Tentando reparar... Erro: {}", e.getMessage());
            try {
                var repairedJson = repairJson(jsonStr);
                analysis = objectMapper.readValue(repairedJson, AnalysisData.class);
                log.info("JSON reparado com sucesso via algoritmo de emergência.");
            } catch (Exception e2) {
                log.error("FALHA CRÍTICA NO PARSE. JSON extraído: \n{}", jsonStr);
                throw e2;
            }
        }

        // Se a IA não enviou bet_suggestion (erro raro), garantir que o objeto exista
        if (analysis.getBetSuggestion() == null) {
            analysis.setBetSuggestion(new AnalysisData.BetSuggestion());
        }

        // Executar cálculos no backend para garantir precisão total
        var prob = analysis.getBetSuggestion().getProbabilityAi();

        // Normalizar: se a IA enviar 90 em vez de 0.9, ou 95 em vez de 0.95
        if (prob > 1.0) {
            prob = prob / 100.0;
        }

        // Evitar divisão por zero e probabilidades irreais (0% ou 100%)
        prob = Math.max(0.01, Math.min(0.99, prob));
        analysis.getBetSuggestion().setProbabilityAi(prob);

        var odd = analysis.getBetSuggestion().getOddBookmaker();

        // SEMPRE sobrescrever cálculos da IA com valores reais do backend
        analysis.getBetSuggestion().setOddFairPoisson(1.0 / prob);

        // Se a odd for 0, o EV não pode ser calculado corretamente
        if (odd > 0) {
            analysis.getBetSuggestion().setExpectedValue((prob * odd) - 1);
            analysis.getBetSuggestion()
                    .setStatusEv(analysis.getBetSuggestion().getExpectedValue() > 0 ? "POSITIVE" : "NEGATIVE");
        } else {
            analysis.getBetSuggestion().setExpectedValue(-1);
            analysis.getBetSuggestion().setStatusEv("NEGATIVE");
        }

        return analysis;
    }

    public static String formatAnalysisToText(AnalysisData data, Fixture fixture) {
        var gameTime = fixture.getDate() != null
                ? fixture.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "--/--/---- --:--";

        var oddStr = data.getBetSuggestion().getOddBookmaker() > 0
                ? String.format("%.2f", data.getBetSuggestion().getOddBookmaker())
                : "Não encontrada";

        var evCalcStr = data.getBetSuggestion().getOddBookmaker() > 0
                ? String.format("(%.0f%% x %.2f) - 1 = %.2f", data.getBetSuggestion().getProbabilityAi() * 100,
                        data.getBetSuggestion().getOddBookmaker(), data.getBetSuggestion().getExpectedValue())
                : "Indisponível (Sem Odd)";

        var evStatus = "POSITIVE".equals(data.getBetSuggestion().getStatusEv())
                && data.getBetSuggestion().getOddBookmaker() > 0 ? "✅ EV Positivo" : "❌ Sem Valor";

        var justification = data.getBetSuggestion().getJustification() != null
                ? "\n**💡 POR QUE ESTA APOSTA?** " + data.getBetSuggestion().getJustification()
                : "";

        return String.format("""
                ### 🎯 SUGESTÃO DE APOSTA (%s x %s - %s)
                **%s** %s
                **Probabilidade da Sugestão:** %.0f%%
                **Odd Justa (Poisson):** %.2f
                **Odd Betano:** %s
                **Cálculo de Valor (EV):** %s (%s)

                **🔥 MERCADO DE GOLS:** %s (%.2f)
                **📊 PROBABILIDADES:** %s: %.0f%% | Empate: %.0f%% | %s: %.0f%%
                **⚽ PLACAR PROVÁVEL:** %s (Odd %.2f)
                **📈 NÍVEL DE CONFIANÇA:** %s""", fixture.getHomeTeam(), fixture.getAwayTeam(), gameTime,
                data.getBetSuggestion().getMarket(), justification, data.getBetSuggestion().getProbabilityAi() * 100,
                data.getBetSuggestion().getOddFairPoisson(), oddStr, evCalcStr, evStatus,
                data.getGoalsMarket().getTarget(), data.getGoalsMarket().getOdd(), fixture.getHomeTeam(),
                data.getProbabilities().getHomeWin() * 100, data.getProbabilities().getDraw() * 100,
                fixture.getAwayTeam(), data.getProbabilities().getAwayWin() * 100,
                data.getPrediction() != null ? data.getPrediction().getCorrectScore() : "N/A",
                data.getPrediction() != null ? data.getPrediction().getScoreOdd() : 0.0,
                data.getProbabilities().getConfidenceLevel());
    }

    public static String formatAnalysisToTelegram(AnalysisData data, Fixture fixture) {
        var gameTime = fixture.getDate() != null
                ? fixture.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "--/--/---- --:--";

        var oddStr = data.getBetSuggestion().getOddBookmaker() > 0
                ? String.format("%.2f", data.getBetSuggestion().getOddBookmaker())
                : "Indisponível";

        return String.format("""
                🎯 SUGESTÃO DE APOSTA (%s x %s - %s)
                *✅ Mercado:* %s
                *🎲 Probabilidade da Sugestão:* %.0f%%
                *💰 Odd Betano:* %s
                *🔥 Mercado de Gols:* %s (%.2f)
                *📊 Probabilidades:* %s: %.0f%% | Empate: %.0f%% | %s: %.0f%%
                *📈 Nível de Confiança:* %s""", fixture.getHomeTeam(), fixture.getAwayTeam(), gameTime,
                data.getBetSuggestion().getMarket(), data.getBetSuggestion().getProbabilityAi() * 100, oddStr,
                data.getGoalsMarket().getTarget(), data.getGoalsMarket().getOdd(), fixture.getHomeTeam(),
                data.getProbabilities().getHomeWin() * 100, data.getProbabilities().getDraw() * 100,
                fixture.getAwayTeam(), data.getProbabilities().getAwayWin() * 100,
                data.getProbabilities().getConfidenceLevel());
    }

    /**
     * Tenta reparar um JSON truncado fechando as chaves e colchetes abertos.
     */
    private static String repairJson(String json) {
        StringBuilder repaired = new StringBuilder(json.trim());

        // Remove vírgulas pendentes no final ou campos vazios (ex: "key": )
        String str = repaired.toString().trim();
        if (str.endsWith(",") || str.endsWith(":")) {
            str = str.substring(0, str.length() - 1).trim();
            repaired = new StringBuilder(str);
        }

        int openBraces = 0;
        int openBrackets = 0;
        boolean inQuote = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"' && (i == 0 || str.charAt(i - 1) != '\\')) {
                inQuote = !inQuote;
            }
            if (!inQuote) {
                if (c == '{')
                    openBraces++;
                if (c == '}')
                    openBraces--;
                if (c == '[')
                    openBrackets++;
                if (c == ']')
                    openBrackets--;
            }
        }

        // Se terminou dentro de uma aspa, fecha ela
        if (inQuote) {
            repaired.append("\"");
        }

        // Fecha colchetes
        while (openBrackets > 0) {
            repaired.append("]");
            openBrackets--;
        }

        // Fecha chaves
        while (openBraces > 0) {
            repaired.append("}");
            openBraces--;
        }

        return repaired.toString();
    }
}
