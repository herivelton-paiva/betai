package br.com.betai.utils;

import br.com.betai.domain.AnalysisData;
import br.com.betai.domain.Fixture;
import br.com.betai.domain.MultiBetResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
        return String.format(
                """
                        Você é um Analista de Apostas e Cientista de Dados especializado em futebol.
                        Sua tarefa: Gerar uma análise técnica em JSON para o jogo %s x %s (%s) em %s.

                        --- CONTEXTO E ODDS ---
                        %s
                        --- ESTATÍSTICAS ---
                        %s
                        --- MODELOS DE REFERÊNCIA ---
                        %s

                        REGRAS DE OURO:
                        1. CONTEXTO CRÍTICO: Use a ferramenta de busca (Search) para verificar se o jogo é ELIMINATÓRIO (Mata-mata) ou se há TIMES RESERVAS. Isso deve ter peso de 80%% na sua decisão.
                        2. MERCADOS PERMITIDOS:
                           - Vitoria do [Nome do Time]
                           - Gols Over/Under (Ex: Mais de 2.5 Gols)
                           - Ambas Marcam (Ex: Ambas Marcam: Sim)
                           - Empate Anula: [Nome do Time]
                           - Vencedor 1º Tempo: [Nome do Time]
                           - Dupla Chance: [Sugestão] (Ex: Dupla Chance: Corinthians ou Empate)
                        3. ESTRATÉGIA DE SEGURANÇA (CRÍTICO):
                           - EQUILÍBRIO: Se a diferença de probabilidade entre Vitória Casa e Fora for menor que 15%%, você DEVE preferir mercados de proteção como "Dupla Chance" ou "Empate Anula".
                           - VALOR: Procure o melhor EV+.
                        4. PROIBIÇÃO DE ZEROS: Os campos 'odd_bookmaker' e 'probability_ai' NUNCA podem ser 0.00. Extraia a odd da seção 'ODDS ATUAIS' ou estime uma odd realista baseada no mercado.
                        5. CONSISTÊNCIA: A 'probability_ai' DEVE ser a soma das probabilidades individuais do mercado (Ex: Dupla Chance = win + draw).
                        6. NÍVEL DE CONFIANÇA (confidence_level):
                           - ALTO: Probabilidade sugerida > 70%% OU forte evidência estatística e contexto claro.
                           - MEDIO: Probabilidade entre 50%% e 70%% OU contexto com algumas incertezas.
                           - BAIXO: Jogos muito equilibrados, mata-mata volátil ou dúvidas sobre escalação reserva.
                        7. FORMATO: JSON puro. NADA de preâmbulos ou markdown. Idioma: Português (Brasil).

                        ESTRUTURA OBRIGATÓRIA (JSON):
                        {
                        "fixture": { "id": %d, "teams": { "home": "%s", "away": "%s" }, "date": "%s" },
                        "bet_suggestion": {
                            "market": "Vitoria do [Nome do Time]",
                            "odd_bookmaker": 1.95,
                            "probability_ai": 0.55,
                            "justification": "justificativa curta aqui, indicar se tiver escalacao reserva"
                        },
                        "goals_market": { "target": "Mais de 1.5 Gols", "odd": 1.80 },
                        "probabilities": { "home_win": 0.55, "draw": 0.25, "away_win": 0.20, "confidence_level": "ALTO" },
                        "prediction": { "correct_score": "2:1", "score_odd": 8.50 }
                        }
                        """,
                home, away, league, date, oddsSection, statistics, predictions, fixtureId, home, away, date);
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

        sanitizeAnalysisData(analysis);

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

    public static void sanitizeAnalysisData(AnalysisData data) {
        if (data == null)
            return;
        if (data.getBetSuggestion() == null) {
            data.setBetSuggestion(new AnalysisData.BetSuggestion());
        }
        if (data.getBetSuggestion().getMarket() == null) {
            data.getBetSuggestion().setMarket("Indisponível");
        }
        if (data.getGoalsMarket() == null) {
            data.setGoalsMarket(new AnalysisData.GoalsMarket());
        }
        if (data.getGoalsMarket().getTarget() == null) {
            data.getGoalsMarket().setTarget("N/A");
        }
        if (data.getProbabilities() == null) {
            data.setProbabilities(new AnalysisData.Probabilities());
        }
        if (data.getProbabilities().getConfidenceLevel() == null) {
            data.getProbabilities().setConfidenceLevel("MEDIO");
        }

        // Correção automática para o erro comum da IA usar o termo genérico "Vencedor
        // 1x2"
        String currentMarket = data.getBetSuggestion().getMarket();
        if (currentMarket != null && (currentMarket.equalsIgnoreCase("Vencedor 1x2")
                || currentMarket.equalsIgnoreCase("1x2") || currentMarket.equalsIgnoreCase("Vencedor"))) {

            if (data.getFixture() != null && data.getFixture().getTeams() != null) {
                if (data.getProbabilities().getHomeWin() > data.getProbabilities().getAwayWin()
                        && data.getProbabilities().getHomeWin() > data.getProbabilities().getDraw()) {
                    data.getBetSuggestion().setMarket("Vencedor: " + data.getFixture().getTeams().getHome());
                } else if (data.getProbabilities().getAwayWin() > data.getProbabilities().getHomeWin()
                        && data.getProbabilities().getAwayWin() > data.getProbabilities().getDraw()) {
                    data.getBetSuggestion().setMarket("Vencedor: " + data.getFixture().getTeams().getAway());
                } else {
                    data.getBetSuggestion().setMarket("Vencedor: Empate");
                }
            } else {
                // Fallback caso o objeto fixture esteja incompleto no JSON
                data.getBetSuggestion().setMarket("Vencedor (Favorito)");
            }
        }

        // Failsafe: Se a probabilidade da sugestão veio zerada mas temos as
        // probabilidades individuais
        if (data.getBetSuggestion().getProbabilityAi() <= 0 && data.getProbabilities() != null) {
            String market = data.getBetSuggestion().getMarket().toLowerCase();
            var p = data.getProbabilities();
            if (market.contains("vitoria") || market.contains("vencedor")) {
                if (data.getFixture() != null && data.getFixture().getTeams() != null) {
                    if (market.contains(data.getFixture().getTeams().getHome().toLowerCase()))
                        data.getBetSuggestion().setProbabilityAi(p.getHomeWin());
                    else if (market.contains(data.getFixture().getTeams().getAway().toLowerCase()))
                        data.getBetSuggestion().setProbabilityAi(p.getAwayWin());
                }
            } else if (market.contains("dupla chance") || market.contains("ou empate")) {
                if (data.getFixture() != null && data.getFixture().getTeams() != null) {
                    if (market.contains(data.getFixture().getTeams().getHome().toLowerCase()))
                        data.getBetSuggestion().setProbabilityAi(p.getHomeWin() + p.getDraw());
                    else if (market.contains(data.getFixture().getTeams().getAway().toLowerCase()))
                        data.getBetSuggestion().setProbabilityAi(p.getAwayWin() + p.getDraw());
                }
            } else if (market.contains("empate anula")) {
                if (data.getFixture() != null && data.getFixture().getTeams() != null) {
                    if (market.contains(data.getFixture().getTeams().getHome().toLowerCase()))
                        data.getBetSuggestion().setProbabilityAi(p.getHomeWin());
                    else if (market.contains(data.getFixture().getTeams().getAway().toLowerCase()))
                        data.getBetSuggestion().setProbabilityAi(p.getAwayWin());
                }
            }
        }
    }

    public static String buildMultiBetPrompt(List<Fixture> analyzedFixtures) {
        String dataJson = analyzedFixtures.stream().map(f -> {
            AnalysisData m = f.getIaAnalysis();
            sanitizeAnalysisData(m);
            String market = m.getBetSuggestion().getMarket();
            double odd = m.getBetSuggestion().getOddBookmaker();
            double prob = m.getBetSuggestion().getProbabilityAi();
            String goalsTarget = m.getGoalsMarket().getTarget() != null ? m.getGoalsMarket().getTarget() : "N/A";
            double goalsOdd = m.getGoalsMarket().getOdd();
            String fullOdds = f.getOdds() != null ? f.getOdds() : "{}";

            return String.format(
                    "{ \"id\": %d, \"home\": \"%s\", \"away\": \"%s\", \"date\": \"%s\", \"suggestion\": \"%s\", \"odd\": %.2f, \"prob\": %.2f, \"goals\": \"%s\", \"goals_odd\": %.2f, \"full_odds_context\": %s }",
                    f.getId(), f.getHomeTeam(), f.getAwayTeam(), f.getDate(), market, odd, prob, goalsTarget, goalsOdd,
                    fullOdds);
        }).collect(Collectors.joining(",\n"));

        return String.format(
                """
                        Você é um Especialista em Combinadas (Múltiplas) de Futebol.
                        Recebi as seguintes análises individuais de IA para partidas de hoje:

                        [
                        %s
                        ]

                        SUA MISSÃO:
                        Baseado nos dados acima, crie EXATAMENTE 4 sugestões de apostas múltiplas com os seguintes tamanhos:
                        - Uma múltipla de 4 jogos (Mais Conservadora/Segura)
                        - Uma múltipla de 6 jogos (Equilibrada)
                        - Uma múltipla de 8 jogos (Arrojada)
                        - Uma múltipla de 10 jogos (High Reward/Zebra Controlada)

                        REGRAS:
                        1. Selecione os melhores jogos para cada perfil.
                        2. MERCADOS: Proibido Handicap. Varie entre Vencedor, Gols e Dupla Chance.
                        3. Calcule a 'final_odd' multiplicando as odds individuais.
                        4. Calcule a 'total_probability' baseada nas probabilidades individuais.
                        5. IDIOMA: Português (Brasil).

                        FORMATO DE SAÍDA:
                        Retorne APENAS o JSON puro seguindo esta estrutura:
                        {
                          "multiples": [
                            {
                              "title": "Múltipla Segura (4 Jogos)",
                              "size": 4,
                              "legs": [
                                { "id_fixture": 123, "team_a": "Time A", "team_b": "Time B", "game_date": "dd/MM HH:mm", "market": "Mercado", "odd": 1.50 }
                              ],
                              "final_odd": 8.50,
                              "total_probability": 0.45
                            }
                          ]
                        }
                        """,
                dataJson);
    }

    public static MultiBetResponse processMultiBetData(String aiResponseRaw, ObjectMapper objectMapper)
            throws JsonProcessingException {
        String raw = aiResponseRaw.replace("```json", "").replace("```", "").trim();
        int firstBrace = raw.indexOf('{');
        int lastBrace = raw.lastIndexOf('}');

        if (firstBrace == -1 || lastBrace == -1) {
            throw new com.fasterxml.jackson.databind.JsonMappingException(null, "JSON de múltiplas não encontrado");
        }

        String jsonStr = raw.substring(firstBrace, lastBrace + 1);
        return objectMapper.readValue(jsonStr, MultiBetResponse.class);
    }

    public static String formatMultipleToText(MultiBetResponse.MultipleSuggestion multiple) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔥 *%s*\n", multiple.getTitle()));

        for (MultiBetResponse.BetLeg leg : multiple.getLegs()) {
            sb.append(String.format("📍 %s | %s x %s\n", leg.getGameDate(), leg.getTeamA(), leg.getTeamB()));
            sb.append(String.format("🎯 %s (@%.2f)\n\n", leg.getMarket(), leg.getOdd()));
        }

        sb.append(String.format("💰 *ODD FINAL: %.2f*\n", multiple.getFinalOdd()));
        sb.append(String.format("📈 Probabilidade Estimada: %.1f%%\n", multiple.getTotalProbability() * 100));

        return sb.toString();
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
