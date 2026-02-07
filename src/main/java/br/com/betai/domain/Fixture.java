package br.com.betai.domain;

import java.time.LocalDateTime;

public class Fixture {
    private Long id;
    private LocalDateTime date;
    private String statusLong;
    private String leagueName;
    private String homeTeam;
    private String awayTeam;
    private Integer homeTeamGoals;
    private Integer awayTeamGoals;
    private String odds;

    public Fixture() {
    }

    public Fixture(Long id, LocalDateTime date, String statusLong, String leagueName, String homeTeam, String awayTeam,
            Integer homeTeamGoals, Integer awayTeamGoals, String odds) {
        this.id = id;
        this.date = date;
        this.statusLong = statusLong;
        this.leagueName = leagueName;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeTeamGoals = homeTeamGoals;
        this.awayTeamGoals = awayTeamGoals;
        this.odds = odds;
    }

    public static FixtureBuilder builder() {
        return new FixtureBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getStatusLong() {
        return statusLong;
    }

    public void setStatusLong(String statusLong) {
        this.statusLong = statusLong;
    }

    public String getLeagueName() {
        return leagueName;
    }

    public void setLeagueName(String leagueName) {
        this.leagueName = leagueName;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(String homeTeam) {
        this.homeTeam = homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(String awayTeam) {
        this.awayTeam = awayTeam;
    }

    public Integer getHomeTeamGoals() {
        return homeTeamGoals;
    }

    public void setHomeTeamGoals(Integer homeTeamGoals) {
        this.homeTeamGoals = homeTeamGoals;
    }

    public Integer getAwayTeamGoals() {
        return awayTeamGoals;
    }

    public void setAwayTeamGoals(Integer awayTeamGoals) {
        this.awayTeamGoals = awayTeamGoals;
    }

    public String getOdds() {
        return odds;
    }

    public void setOdds(String odds) {
        this.odds = odds;
    }

    public static class FixtureBuilder {
        private Long id;
        private LocalDateTime date;
        private String statusLong;
        private String leagueName;
        private String homeTeam;
        private String awayTeam;
        private Integer homeTeamGoals;
        private Integer awayTeamGoals;
        private String odds;

        FixtureBuilder() {
        }

        public FixtureBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public FixtureBuilder date(LocalDateTime date) {
            this.date = date;
            return this;
        }

        public FixtureBuilder statusLong(String statusLong) {
            this.statusLong = statusLong;
            return this;
        }

        public FixtureBuilder leagueName(String leagueName) {
            this.leagueName = leagueName;
            return this;
        }

        public FixtureBuilder homeTeam(String homeTeam) {
            this.homeTeam = homeTeam;
            return this;
        }

        public FixtureBuilder awayTeam(String awayTeam) {
            this.awayTeam = awayTeam;
            return this;
        }

        public FixtureBuilder homeTeamGoals(Integer homeTeamGoals) {
            this.homeTeamGoals = homeTeamGoals;
            return this;
        }

        public FixtureBuilder awayTeamGoals(Integer awayTeamGoals) {
            this.awayTeamGoals = awayTeamGoals;
            return this;
        }

        public FixtureBuilder odds(String odds) {
            this.odds = odds;
            return this;
        }

        public Fixture build() {
            return new Fixture(id, date, statusLong, leagueName, homeTeam, awayTeam, homeTeamGoals, awayTeamGoals,
                    odds);
        }
    }
}
