package br.com.betai.service;

import br.com.betai.domain.Fixture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final DynamoDBService dynamoDBService;
    private final RestTemplate restTemplate;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    public NotificationService(DynamoDBService dynamoDBService, RestTemplate restTemplate) {
        this.dynamoDBService = dynamoDBService;
        this.restTemplate = restTemplate;
    }

    // @Scheduled(cron = "0 * * * * *") // Every minute
    public void sendDailyFixtures() {
        log.info("Starting daily fixtures notification task...");
        LocalDate today = LocalDate.now();
        List<Fixture> fixtures = dynamoDBService.getFixturesByDate(today).stream().map(dynamoDBService::mapToFixture)
                .filter(java.util.Objects::nonNull).toList();

        if (fixtures.isEmpty()) {
            log.info("No fixtures found for today ({}). Skipping notification.", today);
            return;
        }

        log.info("Found {} fixtures for today. Formatting message...", fixtures.size());
        List<String> chunks = new ArrayList<>();
        StringBuilder fullMessageBuilder = new StringBuilder(); // To store the full message for WhatsApp
        StringBuilder currentChunk = new StringBuilder();
        currentChunk.append("⚽ *Partidas do dia ").append(today.toString()).append(" (").append(fixtures.size())
                .append(" Jogos)*\n\n");
        fullMessageBuilder.append("⚽ Partidas do dia ").append(today.toString()).append(" (").append(fixtures.size())
                .append(" Jogos)\n\n");
        // Markdown

        for (Fixture fixture : fixtures) {
            String time = fixture.getDate().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            String entryMarkdown = String.format("🏆 %s\n⚔️ %s x %s\n⏰ %s\n\n", fixture.getLeagueName(),
                    fixture.getHomeTeam(), fixture.getAwayTeam(), time);
            String entryPlain = String.format("🏆 %s\n⚔️ %s x %s\n⏰ %s\n\n", fixture.getLeagueName(),
                    fixture.getHomeTeam(), fixture.getAwayTeam(), time); // For WhatsApp

            if (currentChunk.length() + entryMarkdown.length() > 4000) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder(entryMarkdown);
            } else {
                currentChunk.append(entryMarkdown);
            }
            fullMessageBuilder.append(entryPlain);
        }
        chunks.add(currentChunk.toString());

        log.info("Sending {} message chunks to Telegram...", chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            sendToTelegram(chunks.get(i), i + 1, chunks.size());
        }

        sendToWhatsApp(fullMessageBuilder.toString(), "5511959589952");
    }

    public void sendToTelegram(String message, int part, int total) {
        if ("YOUR_BOT_TOKEN".equals(botToken) || botToken == null || botToken.isEmpty()) {
            log.warn("Telegram Bot Token not configured. Skipping...");
            return;
        }

        String url = "https://api.telegram.org/bot{token}/sendMessage?chat_id={chatId}&text={text}&parse_mode=Markdown";

        try {
            restTemplate.getForObject(url, String.class, botToken, chatId, message);
            log.info("Notification part {}/{} sent to Telegram successfully!", part, total);
        } catch (HttpStatusCodeException e) {
            log.error("Telegram API error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error sending notification to Telegram: {}", e.getMessage());
        }
    }

    private void sendToWhatsApp(String message, String phoneNumber) {
        log.info("Sending WhatsApp message to {}: \n{}", phoneNumber, message);
    }
}
