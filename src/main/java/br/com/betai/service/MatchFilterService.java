package br.com.betai.service;

import br.com.betai.domain.Fixture;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import br.com.betai.domain.AnalysisData;
import br.com.betai.domain.AnalysisContextDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MatchFilterService {
    private static final Logger log = LoggerFactory.getLogger(MatchFilterService.class);
    private final DynamoDBService dynamoDBService;
    private final SqsService sqsService;
    private final ObjectMapper objectMapper;

    public MatchFilterService(DynamoDBService dynamoDBService, SqsService sqsService) {
        this.dynamoDBService = dynamoDBService;
        this.sqsService = sqsService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Filtra os jogos do dia baseando-se em regras estatísticas para selecionar as
     * melhores oportunidades para análise de IA.
     */
    public void filtrarOportunidadesDoDia() {
        LocalDate today = LocalDate.now();
        List<Map<String, AttributeValue>> items = dynamoDBService.getFixturesByDate(today);

        log.info("--- Iniciando Filtragem de Jogos (Data: {}) ---", today);

        List<Map<String, AttributeValue>> filteredItems = items.stream().filter(item -> {
            Fixture fixture = dynamoDBService.mapToFixture(item);
            if (fixture == null)
                return false;

            // Só processar jogos nos status autorizados (NS, AET, 1H, HT, PEN)
            String status = fixture.getStatusShort();
            if (!java.util.Set.of("NS", "AET", "1H", "HT", "PEN", "PST").contains(status)) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        processarItensFiltrados(filteredItems);
    }

    /**
     * Filtra e envia para análise jogos que começam em até 2 horas.
     */
    public void filtrarOportunidadesProximasQuatroHoras() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fourHoursFromNow = now.plus(4, ChronoUnit.HOURS);
        LocalDate today = LocalDate.now();

        log.info("--- Iniciando Filtragem de Próximos Jogos (Próximas 4h: {} até {}) ---", now, fourHoursFromNow);

        List<Map<String, AttributeValue>> items = dynamoDBService.getFixturesByDate(today);

        List<Map<String, AttributeValue>> filteredItems = items.stream().filter(item -> {
            Fixture fixture = dynamoDBService.mapToFixture(item);
            if (fixture == null || fixture.getDate() == null)
                return false;

            // Filtro: Não iniciado AND Horário entre Agora e +4h
            boolean isNS = "NS".equals(fixture.getStatusShort());
            boolean startsSoon = fixture.getDate().isAfter(now) && fixture.getDate().isBefore(fourHoursFromNow);

            return isNS && startsSoon;
        }).collect(Collectors.toList());

        processarItensFiltrados(filteredItems);
    }

    private void processarItensFiltrados(List<Map<String, AttributeValue>> items) {
        int oportunidades = 0;
        int descartados = 0;

        // Ordenar os itens por data do jogo
        items.sort((a, b) -> {
            try {
                String dateA = a.get("fixture").m().get("date").s();
                String dateB = b.get("fixture").m().get("date").s();
                return dateA.compareTo(dateB);
            } catch (Exception e) {
                return 0;
            }
        });

        for (Map<String, AttributeValue> item : items) {
            Fixture fixture = dynamoDBService.mapToFixture(item);

            if (item.containsKey("iaAnalysis")) {
                AnalysisData data = fixture.getIaAnalysis();
                if (data != null && data.getBetSuggestion() != null && data.getProbabilities() != null) {
                    System.out.println(String.format("""
                            ⏭️  %s - %s x %s | ID: %d
                            ✅ Mercado: %s (%.2f)
                            ⚽ Mercado de Gols: %s (%.2f)
                            📊 Probabilidades: Casa: %.0f%% | Empate: %.0f%% | Fora: %.0f%%
                            🤖 Previsão API: %s
                            📝 Previsão IA: %s
                            """, fixture.getLeagueName(), fixture.getHomeTeam(), fixture.getAwayTeam(), fixture.getId(),
                            data.getBetSuggestion().getMarket(), data.getBetSuggestion().getOddBookmaker(),
                            data.getGoalsMarket() != null ? data.getGoalsMarket().getTarget() : "N/A",
                            data.getGoalsMarket() != null ? data.getGoalsMarket().getOdd() : 0.0,
                            data.getProbabilities().getHomeWin() * 100, data.getProbabilities().getDraw() * 100,
                            data.getProbabilities().getAwayWin() * 100,
                            fixture.getPredictionComment() != null ? fixture.getPredictionComment() : "N/A",
                            data.getBetSuggestion().getJustification()));
                } else {
                    System.out.println(String.format("⏭️ [PULANDO] %s x %s | ID: %d | Motivo: Já analisado",
                            fixture.getHomeTeam(), fixture.getAwayTeam(), fixture.getId()));
                }
                continue;
            }

            if (item.containsKey("stats")) {
                try {
                    oportunidades++;
                    System.out.println(String.format("🔥 [INICIANDO ANÁLISE PRÓXIMA] %s x %s | ID: %d",
                            fixture.getHomeTeam(), fixture.getAwayTeam(), fixture.getId()));

                    String statistics = dynamoDBService.convertAttributeValueToJson(item.get("stats"));
                    String predictions = "Previsões não encontradas no DynamoDB.";
                    if (item.containsKey("predictions")) {
                        predictions = dynamoDBService.convertAttributeValueToJson(item.get("predictions"));
                    }

                    AnalysisContextDTO contextPayload = new AnalysisContextDTO(fixture, statistics, predictions);
                    String payloadJson = objectMapper.writeValueAsString(contextPayload);
                    sqsService.sendToAnalysisQueue(payloadJson);

                } catch (Exception e) {
                    log.error("Erro ao processar jogo {}: {}", fixture.getId(), e.getMessage());
                }
            } else {
                descartados++;
                System.out.println(String.format("⚪ [DESCARTADO] %s x %s | ID: %d | Motivo: Sem estatísticas",
                        fixture.getHomeTeam(), fixture.getAwayTeam(), fixture.getId()));
            }
        }
        log.info("--- Filtragem de Próximos Jogos Concluída: {} Oportunidades, {} Descartados ---", oportunidades,
                descartados);
    }
}
