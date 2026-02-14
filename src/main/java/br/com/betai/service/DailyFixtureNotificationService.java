package br.com.betai.service;

import br.com.betai.domain.Fixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DailyFixtureNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DailyFixtureNotificationService.class);
    private final DynamoDBService dynamoDBService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final double DEFAULT_BET = 5.0;
    private static final java.util.Set<String> FINISHED_STATUSES = java.util.Set.of("FT", "AET", "PEN");

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    @Value("${notification.cron.nightly}")
    private String nightlyCron;

    @Value("${notification.cron.morning}")
    private String morningCron;

    public DailyFixtureNotificationService(DynamoDBService dynamoDBService, RestTemplate restTemplate) {
        this.dynamoDBService = dynamoDBService;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        log.info("DailyFixtureNotificationService inicializado. Nightly: {} | Morning: {} (Zone: America/Sao_Paulo)",
                nightlyCron, morningCron);
    }

    @Scheduled(cron = "${notification.cron.nightly:0 0 1 * * *}", zone = "America/Sao_Paulo")
    public void sendBeforeDayFixtures() {
        log.info("Iniciando tarefa agendada de notificação de partidas do dia anterior com os resultados");
        var date = LocalDate.now().minusDays(1);
        getAllMatches(date);
    }

    @Scheduled(cron = "${notification.cron.morning:0 0 2 * * *}", zone = "America/Sao_Paulo")
    public void sendDailyFixtures() {
        log.info("Iniciando tarefa agendada de notificação de partidas do dia...");
        LocalDate today = LocalDate.now();
        getAllMatches(today);
    }

    private void getAllMatches(LocalDate today) {
        List<Fixture> fixtures = dynamoDBService.getFixturesByDate(today).stream().map(dynamoDBService::mapToFixture)
                .filter(java.util.Objects::nonNull).toList();

        if (fixtures.isEmpty()) {
            log.info("Nenhuma partida encontrada para hoje ({}). Pulando notificação.", today);
            return;
        }

        log.info("Encontradas {} partidas para hoje. Agrupando por liga e formatando mensagem...", fixtures.size());

        String dateFormatted = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("⚽ *Jogos do dia - ").append(dateFormatted).append("*\n\n");

        // Ordenar e Agrupar partidas por liga e horário
        java.util.Map<String, List<Fixture>> fixturesByLeague = fixtures.stream()
                .sorted(java.util.Comparator.comparing(Fixture::getLeagueName).thenComparing(Fixture::getDate))
                .collect(java.util.stream.Collectors.groupingBy(Fixture::getLeagueName, java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));

        int totalGames = fixtures.size();
        long greens = 0;
        long reds = 0;
        long aiGreens = 0;
        long aiReds = 0;
        double totalProfit = 0;

        List<String> chunks = new ArrayList<>();

        for (java.util.Map.Entry<String, List<Fixture>> entry : fixturesByLeague.entrySet()) {
            String leagueName = entry.getKey();
            List<Fixture> leagueFixtures = entry.getValue();

            StringBuilder leagueChunk = new StringBuilder();
            leagueChunk.append("🏆 *").append(leagueName).append("*\n");

            for (Fixture fixture : leagueFixtures) {
                String time = fixture.getDate().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                String matchLine;

                boolean isFinished = FINISHED_STATUSES.contains(fixture.getStatusShort());
                String apiIcon = getApiResultIcon(fixture);
                String aiIcon = getAiResultIcon(fixture);
                String combinedIcon = apiIcon + aiIcon;

                double matchReturn = 0;

                if (isFinished && fixture.getHomeTeamGoals() != null && fixture.getAwayTeamGoals() != null) {
                    double oddValue = getRelevantOdd(fixture);
                    boolean isGreen = apiIcon.contains("✅");

                    if (isGreen) {
                        greens++;
                        matchReturn = DEFAULT_BET * (oddValue - 1);
                        totalProfit += matchReturn;
                    } else if (apiIcon.contains("❌")) {
                        reds++;
                        matchReturn = -DEFAULT_BET;
                        totalProfit += matchReturn;
                    }

                    if (aiIcon.contains("✅")) {
                        aiGreens++;
                    } else if (aiIcon.contains("❌")) {
                        aiReds++;
                    }

                    matchLine = String.format("%s%s %d x %d %s - ⏰ %s\n", combinedIcon, fixture.getHomeTeam(),
                            fixture.getHomeTeamGoals(), fixture.getAwayTeamGoals(), fixture.getAwayTeam(), time);
                } else {
                    matchLine = String.format("%s%s x %s - ⏰ %s\n", combinedIcon, fixture.getHomeTeam(),
                            fixture.getAwayTeam(), time);
                }
                leagueChunk.append(matchLine);
            }
            leagueChunk.append("\n");

            if (messageBuilder.length() + leagueChunk.length() > 3000) {
                chunks.add(messageBuilder.toString());
                messageBuilder = new StringBuilder(leagueChunk.toString());
            } else {
                messageBuilder.append(leagueChunk);
            }
        }

        // Adicionar Resumo Estatístico
        long processed = greens + reds;
        long aiProcessed = aiGreens + aiReds;

        if (processed > 0 || aiProcessed > 0) {
            StringBuilder summary = new StringBuilder();
            summary.append("\n📊 *RESUMO DO DIA*\n");
            summary.append("Total de Jogos: ").append(totalGames).append("\n");

            if (processed > 0) {
                double winRate = (double) greens / processed * 100;
                summary.append("\n*Previsões de Especialistas (API):*\n");
                summary.append("✅ Greens: ").append(greens).append("\n");
                summary.append("❌ Reds: ").append(reds).append("\n");
                summary.append(String.format("📈 Acertos: %.1f%%\n", winRate));
                summary.append(
                        String.format("� Saldo: %s R$ %.2f\n", totalProfit >= 0 ? "+" : "-", Math.abs(totalProfit)));
            }

            if (aiProcessed > 0) {
                double aiWinRate = (double) aiGreens / aiProcessed * 100;
                summary.append("\n*Previsões de Inteligência Artificial (IA):*\n");
                summary.append("✅ Greens: ").append(aiGreens).append("\n");
                summary.append("❌ Reds: ").append(aiReds).append("\n");
                summary.append(String.format("📈 Acertos: %.1f%%\n", aiWinRate));
            }

            if (messageBuilder.length() + summary.length() > 4000) {
                chunks.add(messageBuilder.toString());
                messageBuilder = new StringBuilder(summary.toString());
            } else {
                messageBuilder.append(summary);
            }
        }

        chunks.add(messageBuilder.toString());

        log.info("Enviando {} partes para o Telegram...", chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            sendToTelegram(chunks.get(i), i + 1, chunks.size());
        }
    }

    private String getApiResultIcon(Fixture fixture) {
        if (!FINISHED_STATUSES.contains(fixture.getStatusShort()) || fixture.getHomeTeamGoals() == null
                || fixture.getAwayTeamGoals() == null) {
            return "";
        }

        String winnerName = fixture.getWinningTeamName();
        String comment = fixture.getPredictionComment();

        if (winnerName == null && comment == null) {
            return "❓ ";
        }

        int homeGoals = fixture.getHomeTeamGoals();
        int awayGoals = fixture.getAwayTeamGoals();

        boolean isWin = (homeGoals > awayGoals && fixture.getHomeTeam().equals(winnerName))
                || (awayGoals > homeGoals && fixture.getAwayTeam().equals(winnerName));
        boolean isDraw = (homeGoals == awayGoals);

        boolean green = false;

        if (comment != null) {
            String lowerComment = comment.toLowerCase();
            if (lowerComment.contains("draw") || lowerComment.contains("empate")) {
                green = isWin || isDraw;
            } else if (lowerComment.contains("win") || lowerComment.contains("vence")
                    || lowerComment.contains("winner")) {
                green = isWin;
            } else {
                green = winnerName != null && isWin;
            }
        } else {
            green = isWin;
        }

        return green ? "✅ " : "❌ ";
    }

    private String getAiResultIcon(Fixture fixture) {
        if (!FINISHED_STATUSES.contains(fixture.getStatusShort()) || fixture.getHomeTeamGoals() == null
                || fixture.getAwayTeamGoals() == null || fixture.getIaAnalysis() == null) {
            return "";
        }

        br.com.betai.domain.AnalysisData data = fixture.getIaAnalysis();
        br.com.betai.domain.AnalysisData.WinnerNode aiWinner = data.getWinner();
        boolean aiWinOrDraw = data.isWinOrDraw();

        int homeGoals = fixture.getHomeTeamGoals();
        int awayGoals = fixture.getAwayTeamGoals();
        boolean isDraw = (homeGoals == awayGoals);

        Long actualWinnerId = null;
        if (homeGoals > awayGoals)
            actualWinnerId = fixture.getHomeTeamId();
        else if (awayGoals > homeGoals)
            actualWinnerId = fixture.getAwayTeamId();

        boolean green = false;
        boolean hasPrediction = false;

        if (aiWinOrDraw) {
            hasPrediction = true;
            if (aiWinner != null) {
                green = isDraw || (actualWinnerId != null && actualWinnerId.equals(aiWinner.getId()));
            } else {
                green = isDraw;
            }
        } else if (aiWinner != null) {
            hasPrediction = true;
            green = (actualWinnerId != null && actualWinnerId.equals(aiWinner.getId()));
        } else {
            br.com.betai.domain.AnalysisData.GoalsMarket gm = data.getGoalsMarket();
            if (gm != null && gm.getTarget() != null && gm.getOdd() > 0) {
                hasPrediction = true;
                int totalGoals = homeGoals + awayGoals;
                String target = gm.getTarget().toLowerCase();
                if (target.contains("mais de") || target.contains("over")) {
                    double val = parseThreshold(target);
                    green = totalGoals > val;
                } else if (target.contains("menos de") || target.contains("under")) {
                    double val = parseThreshold(target);
                    green = totalGoals < val;
                } else if (target.contains("ambas") || target.contains("both")) {
                    green = homeGoals > 0 && awayGoals > 0;
                } else {
                    hasPrediction = false;
                }
            }
        }

        if (!hasPrediction)
            return "";
        return green ? "✅ " : "❌ ";
    }

    private double parseThreshold(String text) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[0-9]+(\\.[0-9]+)?").matcher(text);
            if (m.find()) {
                return Double.parseDouble(m.group());
            }
        } catch (Exception e) {
            log.warn("Erro ao extrair valor de threshold: {}", text);
        }
        return 0.0;
    }

    private double getRelevantOdd(Fixture fixture) {
        if (fixture.getOdds() == null || fixture.getOdds().isEmpty()) {
            return 0.0;
        }

        try {
            JsonNode root = objectMapper.readTree(fixture.getOdds());
            JsonNode bets = null;

            if (root.has("bookmaker")) {
                bets = root.path("bookmaker").path("bets");
            } else if (root.has("bookmakers") && root.get("bookmakers").isArray()) {
                bets = root.get("bookmakers").get(0).path("bets");
            }

            if (bets == null || !bets.isArray()) {
                return 0.0;
            }

            for (JsonNode bet : bets) {
                if (bet.path("id").asInt() == 1 || "Match Winner".equalsIgnoreCase(bet.path("name").asText())) {
                    JsonNode values = bet.path("values");
                    String winnerName = fixture.getWinningTeamName();

                    String selection = "Draw";
                    if (fixture.getHomeTeam().equals(winnerName)) {
                        selection = "Home";
                    } else if (fixture.getAwayTeam().equals(winnerName)) {
                        selection = "Away";
                    }

                    for (JsonNode value : values) {
                        if (selection.equalsIgnoreCase(value.path("value").asText())) {
                            return value.path("odd").asDouble();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar odds para a partida {}: {}", fixture.getId(), e.getMessage());
        }
        return 0.0;
    }

    private void sendToTelegram(String message, int part, int total) {
        if ("YOUR_BOT_TOKEN".equals(botToken) || botToken == null || botToken.isEmpty()) {
            log.warn("Telegram Bot Token não configurado. Pulando envio...");
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", message);
        body.put("parse_mode", "Markdown");

        try {
            restTemplate.postForEntity(url, body, String.class);
        } catch (HttpStatusCodeException e) {
            log.error("Erro na API do Telegram ({}): {} - Body snippet: {}", e.getStatusCode(),
                    e.getResponseBodyAsString(), message.substring(0, Math.min(message.length(), 100)));
        } catch (Exception e) {
            log.error("Erro ao enviar notificação para o Telegram (Parte {}): {}", part, e.getMessage());
        }
    }
}
