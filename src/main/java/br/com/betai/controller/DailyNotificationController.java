package br.com.betai.controller;

import br.com.betai.service.DailyFixtureNotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/daily")
public class DailyNotificationController {

    private final DailyFixtureNotificationService dailyFixtureNotificationService;

    public DailyNotificationController(DailyFixtureNotificationService dailyFixtureNotificationService) {
        this.dailyFixtureNotificationService = dailyFixtureNotificationService;
    }

    @GetMapping("/test")
    public String triggerTest() {
        dailyFixtureNotificationService.sendDailyFixtures();
        return "Processo de notificação diária disparado com sucesso!";
    }
}
