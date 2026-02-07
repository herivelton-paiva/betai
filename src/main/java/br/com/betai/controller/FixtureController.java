package br.com.betai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.betai.domain.Fixture;
import br.com.betai.service.DynamoDBService;
import br.com.betai.service.GeminiAnalysisService;
import br.com.betai.service.NotificationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fixtures")
public class FixtureController {

    private static final Logger log = LoggerFactory.getLogger(FixtureController.class);

    private final NotificationService notificationService;
    private final GeminiAnalysisService geminiAnalysisService;
    private final DynamoDBService dynamoDBService;

    public FixtureController(NotificationService notificationService, GeminiAnalysisService geminiAnalysisService,
            DynamoDBService dynamoDBService) {
        this.notificationService = notificationService;
        this.geminiAnalysisService = geminiAnalysisService;
        this.dynamoDBService = dynamoDBService;
    }

    @GetMapping
    public List<Fixture> getFixtures(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate searchDate = (date != null) ? date : LocalDate.now();
        return dynamoDBService.getFixturesByDate(searchDate).stream().map(dynamoDBService::mapToFixture)
                .filter(java.util.Objects::nonNull).toList();
    }

    @GetMapping("/notify")
    public String triggerNotification() {
        notificationService.sendDailyFixtures();
        return "Notification process triggered!";
    }

    @GetMapping("/{id}/analyze")
    public String analyzeFixture(@PathVariable Long id) {
        // Consulta DynamoDB que já contém todas as informações (ficha do jogo,
        // estatísticas e previsões)
        java.util.Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> data = dynamoDBService
                .getFixtureData(id);

        if (data == null) {
            return "Fixture não encontrada no DynamoDB";
        }

        // Mapeia os dados da partida diretamente do JSON do DynamoDB
        Fixture fixture = dynamoDBService.mapToFixture(data);

        if (fixture == null) {
            return "Dados da partida (fixture) não encontrados no registro do DynamoDB";
        }

        String statistics = "Estatísticas não encontradas no DynamoDB.";
        String predictions = "Previsões não encontradas no DynamoDB.";

        if (data.containsKey("stats")) {
            statistics = dynamoDBService.convertAttributeValueToJson(data.get("stats"));
        }
        if (data.containsKey("predictions")) {
            predictions = dynamoDBService.convertAttributeValueToJson(data.get("predictions"));
        }

        log.info("Fixture recuperada: {} x {} ({})", fixture.getHomeTeam(), fixture.getAwayTeam(),
                fixture.getLeagueName());

        return geminiAnalysisService.analyzeWithContext(fixture, statistics, predictions);
    }
}
