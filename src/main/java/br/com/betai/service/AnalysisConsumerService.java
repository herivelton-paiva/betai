package br.com.betai.service;

import br.com.betai.domain.AnalysisContextDTO;
import br.com.betai.utils.AnalysisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import io.awspring.cloud.sqs.annotation.SqsListener;

@Service
public class AnalysisConsumerService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisConsumerService.class);
    private final GeminiAnalysisService geminiAnalysisService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public AnalysisConsumerService(GeminiAnalysisService geminiAnalysisService,
            NotificationService notificationService) {
        this.geminiAnalysisService = geminiAnalysisService;
        this.notificationService = notificationService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @SqsListener(value = "match-analyser-betai", pollTimeoutSeconds = "20", maxConcurrentMessages = "1")
    public void consumeAnalysisPayload(String payload) {
        log.info("SQS: Nova mensagem recebida. Iniciando processamento...");
        try {
            var context = objectMapper.readValue(payload, AnalysisContextDTO.class);
            var fixture = context.getFixture();

            log.info("Processando análise via contexto: {} (ID: {})",
                    fixture.getHomeTeam() + " vs " + fixture.getAwayTeam(), fixture.getId());

            var analysis = geminiAnalysisService.analyzeWithContextDetailed(fixture, context.getStatistics(),
                    context.getPredictions());

            if (analysis != null) {
                // Só envia se o EV for positivo
                if ("POSITIVE".equals(analysis.getBetSuggestion().getStatusEv())
                        && analysis.getBetSuggestion().getOddBookmaker() > 0) {

                    String telegramMessage = AnalysisUtils.formatAnalysisToTelegram(analysis, fixture);
                    log.info("Enviando análise para o Telegram...");
                    notificationService.sendToTelegram(telegramMessage, 1, 1);

                } else {
                    log.info("Análise da partida {} descartada por falta de EV positivo.", fixture.getId());
                }
            }

            log.info("Aguardando 30 segundos antes de processar a próxima mensagem...");
            Thread.sleep(30000);
            log.info("Pausa concluída. Finalizando processamento desta mensagem.");

        } catch (InterruptedException e) {
            log.warn("Processamento SQS interrompido: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Erro ao processar mensagem do SQS: {}", e.getMessage());
        }
    }
}
