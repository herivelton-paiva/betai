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
                        Você é um Especialista Sênior em Apostas Esportivas e Cientista de Dados. Sua reputação depende da ACERTIVIDADE técnica.
                        Sua tarefa: Gerar uma análise técnica INDEPENDENTE e REALISTA em JSON para o jogo %s x %s (%s) em %s.

                        --- CONTEXTO E ODDS ---
                        %s
                        --- ESTATÍSTICAS ---
                        %s
                        --- MODELOS DE REFERÊNCIA (APENAS APOIO) ---
                        %s

                        REGRAS DE OURO PARA ACERTIVIDADE (MÁXIMA PRIORIDADE):
                        1. LÓGICA DE MERCADO E REALIDADE: As odds das casas de apostas são o melhor indicador de qualidade técnica. NÃO subestime favoritos (Ex: Real Madrid, Man City, Juventus) sem provas IRREFUTÁVEIS. Se o mercado dá @1.40 para um time, ele é o favorito técnico. Você só pode ir contra se encontrar notícias de última hora (Search) que o mercado ainda não absorveu (Ex: 5 titulares fora).
                        2. NOTÍCIAS REAIS (FUNDAMENTAL): Use o Search para buscar: "[Time] desfalques hoje", "[Time] lineup news". A ausência de um craque pesa mais que "histórico de confrontos de 5 anos atrás".
                        3. CENÁRIO DE MATA-MATA (IDA/VOLTA): Obrigatoriamente pesquise: "Resultado de [TimeA] vs [TimeB] ida". Se for jogo de volta, o time que venceu a ida pode jogar de forma defensiva/pelo empate. Isso muda totalmente a 'probability_ai'.
                        4. ESTRATÉGIA DE SEGURANÇA E MERCADOS: Prefira mercados com alta probabilidade de acerto (Win Rate). Além de "Dupla Chance" e "Empate Anula", explore mercados de gols conservadores como "Mais de 0.5 Gols" ou "Menos de 3.5 Gols". O objetivo é ACERTAR.
                        5. FATOR CASA E AMBIENTE: Considere o peso de jogar em casa, especialmente em ligas com torcidas fanáticas (ex: Turquia, Brasil, Grécia). Um favorito técnico jogando fora de casa em um ambiente hostil tem sua probabilidade reduzida.
                        6. CONSISTÊNCIA MATEMÁTICA: A 'probability_ai' deve ser realista. Se você der 45%% de vitória para um azarão com odd @3.85, sua lógica está FALHA (45%% equivale a uma odd @2.22). Alinhe suas probabilidades com a realidade do mercado.
                        7. JUSTIFICATIVA "SEM ENROLAÇÃO": Foque em fatos: "X jogadores lesionados", "placar de ida foi 2-1", "time B joga pelo empate". Evite termos vagos como "motivação alta" se o time for tecnicamente inferior.
                        8. FORMATO: JSON puro. Idioma: Português (Brasil).

                        ESTRUTURA OBRIGATÓRIA (JSON):
                        {
                        "fixture": { "id": %d, "teams": { "home": "%s", "away": "%s" }, "date": "%s" },
                        "bet_suggestion": {
                            "market": "Mercado Selecionado",
                            "odd_bookmaker": 1.95,
                            "probability_ai": 0.55,
                            "justification": "Fatos reais: notícias de desfalques, situação do mata-mata (placar ida) e por que este mercado é o mais SEGURO."
                        },
                        "goals_market": { "target": "Mais/Menos X.5 Gols", "odd": 1.80 },
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
                        Baseado nos dados acima, crie EXATAMENTE 2 sugestões de apostas múltiplas:
                        1. Múltipla Cautelosa (4 Jogos): Foque em jogos com maior probabilidade (prob > 0.70) e odds mais seguras.
                        2. Múltipla Agressiva (5 Jogos): Busque maior retorno, podendo incluir odds mais altas e mercados mais ousados.

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
                              "title": "Múltipla Cautelosa (4 Jogos)",
                              "size": 4,
                              "legs": [
                                { "id_fixture": 123, "team_a": "Time A", "team_b": "Time B", "game_date": "dd/MM HH:mm", "market": "Mercado", "odd": 1.50 }
                              ],
                              "final_odd": 5.50,
                              "total_probability": 0.35
                            },
                            {
                              "title": "Múltipla Agressiva (5 Jogos)",
                              "size": 5,
                              "legs": [
                                { "id_fixture": 456, "team_a": "Time C", "team_b": "Time D", "game_date": "dd/MM HH:mm", "market": "Mercado", "odd": 2.10 }
                              ],
                              "final_odd": 15.80,
                              "total_probability": 0.15
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

        var mainOddStr = data.getBetSuggestion().getOddBookmaker() > 0
                ? String.format("%.2f", data.getBetSuggestion().getOddBookmaker())
                : "?.??";

        var goalsOddStr = data.getGoalsMarket().getOdd() > 0 ? String.format("%.2f", data.getGoalsMarket().getOdd())
                : "?.??";

        return String.format("""
                🎯 SUGESTÃO DE APOSTA (%s x %s - %s)
                🎲 Probabilidade da Sugestão: %.0f%%
                ✅ Mercado: %s (%s)
                ⚽ Mercado de Gols: %s (%s)
                📊 Probabilidades: Casa: %.0f%% | Empate: %.0f%% | Fora: %.0f%%""", fixture.getHomeTeam(),
                fixture.getAwayTeam(), gameTime, data.getBetSuggestion().getProbabilityAi() * 100,
                data.getBetSuggestion().getMarket(), mainOddStr, data.getGoalsMarket().getTarget(), goalsOddStr,
                data.getProbabilities().getHomeWin() * 100, data.getProbabilities().getDraw() * 100,
                data.getProbabilities().getAwayWin() * 100);
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
