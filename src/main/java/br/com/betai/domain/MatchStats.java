package br.com.betai.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchStats {
    private TeamStats home;
    private TeamStats away;

    public MatchStats() {
    }

    public MatchStats(TeamStats home, TeamStats away) {
        this.home = home;
        this.away = away;
    }

    public TeamStats getHome() {
        return home;
    }

    public void setHome(TeamStats home) {
        this.home = home;
    }

    public TeamStats getAway() {
        return away;
    }

    public void setAway(TeamStats away) {
        this.away = away;
    }
}
