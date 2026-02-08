package br.com.betai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import br.com.betai.domain.Fixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.HashMap;

@Service
public class DynamoDBService {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBService.class);
    private final DynamoDbClient dynamoDbClient;
    private final ObjectMapper objectMapper;
    private final String tableName = "BettingFixtures";

    public DynamoDBService() {
        this.dynamoDbClient = DynamoDbClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create()).build();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, AttributeValue> getFixtureData(Long fixtureId) {
        log.info("Consultando DynamoDB para a partida: {}", fixtureId);

        try {
            GetItemRequest request = GetItemRequest.builder().tableName(tableName)
                    .key(Map.of("fixtureId", AttributeValue.builder().s(String.valueOf(fixtureId)).build())).build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (!response.hasItem()) {
                log.warn("Partida {} não encontrada no DynamoDB", fixtureId);
                return null;
            }

            return response.item();
        } catch (Exception e) {
            log.error("Erro ao consultar DynamoDB para a partida: {}", fixtureId, e);
            throw new RuntimeException("Falha ao consultar DynamoDB: " + e.getMessage(), e);
        }
    }

    public String convertAttributeValueToJson(AttributeValue attributeValue) {
        if (attributeValue == null)
            return "{}";
        try {
            return objectMapper.writeValueAsString(convertToRawMap(attributeValue));
        } catch (Exception e) {
            log.error("Erro ao converter AttributeValue para JSON", e);
            return "{}";
        }
    }

    private Object convertToRawMap(AttributeValue v) {
        if (v.hasM()) {
            Map<String, Object> map = new HashMap<>();
            v.m().forEach((key, val) -> map.put(key, convertToRawMap(val)));
            return map;
        } else if (v.hasL()) {
            return v.l().stream().map(this::convertToRawMap).toList();
        } else if (v.s() != null) {
            return v.s();
        } else if (v.n() != null) {
            return v.n();
        } else if (v.bool() != null) {
            return v.bool();
        }
        return null;
    }

    public java.util.List<Map<String, AttributeValue>> getFixturesByDate(java.time.LocalDate date) {
        log.info("Buscando jogos no DynamoDB para a data: {}", date);
        String dateStr = date.toString();

        try {
            software.amazon.awssdk.services.dynamodb.model.ScanRequest scanRequest = software.amazon.awssdk.services.dynamodb.model.ScanRequest
                    .builder().tableName(tableName).filterExpression("begins_with(gameDate, :date)")
                    .expressionAttributeValues(Map.of(":date", AttributeValue.builder().s(dateStr).build())).build();

            software.amazon.awssdk.services.dynamodb.model.ScanResponse response = dynamoDbClient.scan(scanRequest);
            return response.items();
        } catch (Exception e) {
            log.error("Erro ao buscar jogos por data no DynamoDB", e);
            throw new RuntimeException("Falha ao buscar jogos no DynamoDB: " + e.getMessage(), e);
        }
    }

    public Fixture mapToFixture(Map<String, AttributeValue> data) {
        if (data == null || !data.containsKey("fixture")) {
            return null;
        }

        Map<String, AttributeValue> fixtureMap = data.get("fixture").m();

        Fixture.FixtureBuilder builder = Fixture.builder();

        // ID
        if (fixtureMap.containsKey("fixtureId")) {
            builder.id(Long.valueOf(fixtureMap.get("fixtureId").n()));
        }

        // Date
        if (fixtureMap.containsKey("date")) {
            builder.date(OffsetDateTime.parse(fixtureMap.get("date").s()).toLocalDateTime());
        }

        // Teams
        if (fixtureMap.containsKey("teams")) {
            Map<String, AttributeValue> teams = fixtureMap.get("teams").m();
            if (teams.containsKey("home")) {
                builder.homeTeam(teams.get("home").m().get("name").s());
            }
            if (teams.containsKey("away")) {
                builder.awayTeam(teams.get("away").m().get("name").s());
            }
        }

        // League
        if (fixtureMap.containsKey("league")) {
            builder.leagueName(fixtureMap.get("league").m().get("name").s());
        }

        // Status
        if (fixtureMap.containsKey("status")) {
            Map<String, AttributeValue> status = fixtureMap.get("status").m();
            if (status.containsKey("long")) {
                builder.statusLong(status.get("long").s());
            }
            if (status.containsKey("short")) {
                builder.statusShort(status.get("short").s());
            }
        }

        // Goals / Score
        if (fixtureMap.containsKey("goals")) {
            Map<String, AttributeValue> goals = fixtureMap.get("goals").m();
            if (goals.containsKey("home") && goals.get("home").n() != null) {
                builder.homeTeamGoals(Integer.valueOf(goals.get("home").n()));
            }
            if (goals.containsKey("away") && goals.get("away").n() != null) {
                builder.awayTeamGoals(Integer.valueOf(goals.get("away").n()));
            }
        } else if (fixtureMap.containsKey("score")) {
            Map<String, AttributeValue> score = fixtureMap.get("score").m();
            if (score.containsKey("home") && score.get("home").n() != null) {
                builder.homeTeamGoals(Integer.valueOf(score.get("home").n()));
            }
            if (score.containsKey("away") && score.get("away").n() != null) {
                builder.awayTeamGoals(Integer.valueOf(score.get("away").n()));
            }
        }

        // Odds (can be a separate column in the main data map)
        if (data.containsKey("odds")) {
            builder.odds(convertAttributeValueToJson(data.get("odds")));
        }

        // Predictions
        if (data.containsKey("predictions")) {
            Map<String, AttributeValue> predictionsMap = data.get("predictions").m();
            if (predictionsMap.containsKey("predictions")) {
                Map<String, AttributeValue> innerPredictions = predictionsMap.get("predictions").m();
                if (innerPredictions.containsKey("winner")) {
                    Map<String, AttributeValue> winner = innerPredictions.get("winner").m();
                    if (winner.containsKey("name")) {
                        builder.winningTeamName(winner.get("name").s());
                    }
                    if (winner.containsKey("comment")) {
                        builder.predictionComment(winner.get("comment").s());
                    }
                }
            }
        }

        return builder.build();
    }
}
