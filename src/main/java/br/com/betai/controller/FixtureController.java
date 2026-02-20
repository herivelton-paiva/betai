package br.com.betai.controller;

import br.com.betai.domain.Fixture;
import br.com.betai.service.DynamoDBService;
import br.com.betai.service.GeminiAnalysisService;
import br.com.betai.service.MatchFilterService;
import br.com.betai.service.MultiBetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fixtures")
public class FixtureController {

    private final DynamoDBService dynamoDBService;
    private final MatchFilterService matchFilterService;
    private final GeminiAnalysisService geminiAnalysisService;
    private final MultiBetService multiBetService;

    public FixtureController(DynamoDBService dynamoDBService, MatchFilterService matchFilterService,
            GeminiAnalysisService geminiAnalysisService, MultiBetService multiBetService) {
        this.dynamoDBService = dynamoDBService;
        this.matchFilterService = matchFilterService;
        this.geminiAnalysisService = geminiAnalysisService;
        this.multiBetService = multiBetService;
    }

    @GetMapping
    public List<Fixture> getFixtures(@RequestParam(required = false) String date) {
        LocalDate localDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        return dynamoDBService.getFixturesByDateMapped(localDate);
    }

    @GetMapping("/filter")
    public String filterFixtures() {
        matchFilterService.filtrarOportunidadesDoDia();
        return "Processamento de filtro iniciado e mensagens enviadas para o SQS.";
    }

    @GetMapping("/filter-upcoming")
    public String filterUpcomingFixtures() {
        matchFilterService.filtrarOportunidadesProximasQuatroHoras();
        return "Processamento de filtro para as próximas 2 horas iniciado.";
    }

    @GetMapping("/multiples")
    public String generateMultiples() {
        multiBetService.generateAndSendMultiples();
        return "Processamento de múltiplas iniciado.";
    }

    @GetMapping("/{id}/analyze")
    public String analyzeFixture(@PathVariable Long id) {
        Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> data = dynamoDBService
                .getFixtureData(id);
        if (data == null) {
            return "Partida não encontrada no DynamoDB.";
        }

        Fixture fixture = dynamoDBService.mapToFixture(data);
        String statistics = dynamoDBService.convertAttributeValueToJson(data.get("statistics"));
        String predictions = dynamoDBService.convertAttributeValueToJson(data.get("predictions"));

        return geminiAnalysisService.analyzeWithContext(fixture, statistics, predictions);
    }
}
