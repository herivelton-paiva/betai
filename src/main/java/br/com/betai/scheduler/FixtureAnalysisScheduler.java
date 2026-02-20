package br.com.betai.scheduler;

import br.com.betai.service.MatchFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FixtureAnalysisScheduler {

    private static final Logger log = LoggerFactory.getLogger(FixtureAnalysisScheduler.class);
    private final MatchFilterService matchFilterService;

    public FixtureAnalysisScheduler(MatchFilterService matchFilterService) {
        this.matchFilterService = matchFilterService;
    }

    /**
     * Roda a cada 30 minutos para analisar jogos que começam em até 4 horas. Cron:
     * Segundo Minuto Hora DiaMes Mes DiaSemana
     */
    @Scheduled(cron = "0 0/30 * * * *")
    public void scheduleUpcomingMatchesAnalysis() {
        log.info("Executando análise agendada de jogos próximos (30 min check)...");
        try {
            matchFilterService.filtrarOportunidadesProximasQuatroHoras();
        } catch (Exception e) {
            log.error("Erro na execução do scheduler de análise: {}", e.getMessage());
        }
    }
}
