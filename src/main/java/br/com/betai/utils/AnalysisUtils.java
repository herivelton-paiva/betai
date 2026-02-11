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
        return String.format(
                """
                        Você é um Analista de Apostas Profissional e Cientista de Dados.
                        Analise o confronto %s x %s (%s) em %s.

                        --- 📑 CONTEXTO E ODDS ATUAIS ---
                        %s
                        --- 📊 ESTATÍSTICAS HISTÓRICAS ---
                        %s
                        --- 🤖 PREVISÕES DE REFERÊNCIA (ESTATÍSTICA PURA) ---
                        %s
                        *Nota: Estas previsões são baseadas apenas em modelos matemáticos (Poisson/ELO) e servem apenas como ponto de partida.*

                        ⚠️ INSTRUÇÕES DE MISSÃO (CRÍTICO):
                        1. INDEPENDÊNCIA ANALÍTICA: Você NÃO deve apenas replicar as 'PREVISÕES DE REFERÊNCIA'. Seu trabalho é ser um analista crítico. Se sua pesquisa sobre notícias, escalações e contexto sugerir um caminho diferente dos modelos matemáticos, sua análise INDEPENDENTE deve prevalecer. Use as previsões base apenas como contexto inicial.

                        2. PESQUISA EM TEMPO REAL: Use o Google Search para verificar:
                        - Escalações: Há indícios de time reserva ou poupado devido a calendário (Libertadores, finais)?
                        - Desfalques: Lesões de jogadores-chave (artilheiro, goleiro titular, capitão)?
                        - Ambiente: O jogo será em altitude, clima extremo ou campo neutro?

                        3. LÓGICA ESTATÍSTICA AVANÇADA:
                        - Compare 'home.goals.against.average.home' com 'away.goals.for.average.away'. Se o ataque do visitante for superior à defesa do mandante em casa, isso justifica um aumento no 'confidence_level' para mercados como 'Ambas Marcam' ou 'Over Gols'.
                        - Correlacione 'away.fixtures.loses.away' com o favoritismo das odds. Se o visitante perde muito fora mas as odds estão esmagadoramente a favor dele, use isso para decidir se há valor (EV+) real ou se é uma "trap", ajustando a confiança da análise.

                        4. AJUSTE DE PROBABILIDADE: A 'probability_ai' deve ser o resultado final do seu raciocínio (Estatística + Notícias + Contexto).
                        - Se os dados matemáticos apontam favoritismo, mas sua pesquisa indica time reserva, mude a sugestão de aposta para buscar o valor real (EV+).

                        5. PIVOTAGEM PARA EV+: Se o mercado de 'Vencedor' tiver EV negativo, você DEVE vasculhar as 'ODDS ATUAIS' para encontrar mercados de 'Dupla Chance', 'Ambas Marcam' ou 'Gols Over/Under' que apresentem desajuste a favor do apostador.

                        6. CÁLCULO DE PROBABILIDADE DO MERCADO:
                        - Para mercados de 'Dupla Chance' (ex: 1X), a 'probability_ai' DEVE ser a soma das probabilidades individuais (Home Win + Draw). Ex: Se Home Win é 0.45 e Draw é 0.45, a 'probability_ai' para 1X DEVE ser 0.90.
                        - NUNCA use a probabilidade de apenas um resultado para um mercado que engloba dois ou mais.

                        7. REGRAS DE SAÍDA (OBRIGATÓRIO):
                        - Seu output DEVE ser APENAS o objeto JSON puro.
                        - NÃO inclua preâmbulos, explicações, saudações ou "Aqui está sua análise".
                        - NÃO use blocos de código markdown (como ```json). Comece diretamente com { e termine com }.
                        - Linguagem: Português Brasil.

                        ESTRUTURA DO JSON:
                        {
                        "fixture": { "id": %d, "teams": { "home": "%s", "away": "%s" }, "date": "%s" },
                        "bet_suggestion": {
                            "market": "Nome do Mercado",
                            "odd_bookmaker": 0.00,
                            "probability_ai": 0.00,
                            "justification": "Explicação técnica curta (notícias/clima/escalação)"
                        },
                        "goals_market": { "target": "Mercado de Gols", "odd": 0.00 },
                        "probabilities": { "home_win": 0.00, "draw": 0.00, "away_win": 0.00, "confidence_level": "BAIXO/MEDIO/ALTO" },
                        "prediction": { "correct_score": "X:Y", "score_odd": 0.00 }
                        }
                        """,
                home, away, league, date, oddsSection, statistics, predictions, fixtureId, home, away, date);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AnalysisUtils.class);

    public static AnalysisData processAnalysisData(String aiResponseRaw, ObjectMapper objectMapper)
            throws JsonProcessingException {
        // Extrair JSON da resposta buscando do primeiro { ao último }
        int firstBrace = aiResponseRaw.indexOf('{');
        int lastBrace = aiResponseRaw.lastIndexOf('}');

        if (firstBrace == -1 || lastBrace == -1 || lastBrace < firstBrace) {
            log.error("Resposta da IA não contém um bloco JSON válido: {}", aiResponseRaw);
            throw new com.fasterxml.jackson.databind.JsonMappingException(null,
                    "Formato JSON não encontrado na resposta da IA");
        }

        var jsonStr = aiResponseRaw.substring(firstBrace, lastBrace + 1);

        // Limpeza de erros comuns de JSON gerados por IA (como vírgulas extras no final
        // de objetos)
        jsonStr = jsonStr.replaceAll(",\\s*([}\\]])", "$1");

        AnalysisData analysis;
        try {
            analysis = objectMapper.readValue(jsonStr, AnalysisData.class);
        } catch (Exception e) {
            log.warn("Falha ao ler JSON (possivelmente truncado). Tentando reparar... Erro: {}", e.getMessage());
            try {
                var repairedJson = repairJson(jsonStr);
                analysis = objectMapper.readValue(repairedJson, AnalysisData.class);
                log.info("JSON reparado com sucesso.");
            } catch (Exception e2) {
                log.error("Falha ao processar JSON mesmo após tentativa de reparo. JSON Bruto extraído: \n{}", jsonStr);
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
