package br.com.betai.domain;

import java.time.LocalDateTime;

public class Fixture {
    private Long id;
    private LocalDateTime date;
    private String statusLong;
    private String statusShort;
    private String leagueName;
    private String homeTeam;
    private Long homeTeamId;
    private String awayTeam;
    private Long awayTeamId;
    private Integer homeTeamGoals;
    private Integer awayTeamGoals;
    private String odds;
    private String winningTeamName;
    private String predictionComment;
    private AnalysisData iaAnalysis;

    public Fixture() {
    }

    public Fixture(Long id, LocalDateTime date, String statusLong, String statusShort, String leagueName,
            String homeTeam, Long homeTeamId, String awayTeam, Long awayTeamId, Integer homeTeamGoals,
            Integer awayTeamGoals, String odds, String winningTeamName, String predictionComment,
            AnalysisData iaAnalysis) {
        this.id = id;
        this.date = date;
        this.statusLong = statusLong;
        this.statusShort = statusShort;
        this.leagueName = leagueName;
        this.homeTeam = homeTeam;
        this.homeTeamId = homeTeamId;
        this.awayTeam = awayTeam;
        this.awayTeamId = awayTeamId;
        this.homeTeamGoals = homeTeamGoals;
        this.awayTeamGoals = awayTeamGoals;
        this.odds = odds;
        this.winningTeamName = winningTeamName;
        this.predictionComment = predictionComment;
        this.iaAnalysis = iaAnalysis;
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

    public String getStatusShort() {
        return statusShort;
    }

    public void setStatusShort(String statusShort) {
        this.statusShort = statusShort;
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

    public Long getHomeTeamId() {
        return homeTeamId;
    }

    public void setHomeTeamId(Long homeTeamId) {
        this.homeTeamId = homeTeamId;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(String awayTeam) {
        this.awayTeam = awayTeam;
    }

    public Long getAwayTeamId() {
        return awayTeamId;
    }

    public void setAwayTeamId(Long awayTeamId) {
        this.awayTeamId = awayTeamId;
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

    public String getWinningTeamName() {
        return winningTeamName;
    }

    public void setWinningTeamName(String winningTeamName) {
        this.winningTeamName = winningTeamName;
    }

    public String getPredictionComment() {
        return predictionComment;
    }

    public void setPredictionComment(String predictionComment) {
        this.predictionComment = predictionComment;
    }

    public AnalysisData getIaAnalysis() {
        return iaAnalysis;
    }

    public void setIaAnalysis(AnalysisData iaAnalysis) {
        this.iaAnalysis = iaAnalysis;
    }

    public static class FixtureBuilder {
        private Long id;
        private LocalDateTime date;
        private String statusLong;
        private String statusShort;
        private String leagueName;
        private String homeTeam;
        private Long homeTeamId;
        private String awayTeam;
        private Long awayTeamId;
        private Integer homeTeamGoals;
        private Integer awayTeamGoals;
        private String odds;
        private String winningTeamName;
        private String predictionComment;
        private AnalysisData iaAnalysis;

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

        public FixtureBuilder statusShort(String statusShort) {
            this.statusShort = statusShort;
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

        public FixtureBuilder homeTeamId(Long homeTeamId) {
            this.homeTeamId = homeTeamId;
            return this;
        }

        public FixtureBuilder awayTeam(String awayTeam) {
            this.awayTeam = awayTeam;
            return this;
        }

        public FixtureBuilder awayTeamId(Long awayTeamId) {
            this.awayTeamId = awayTeamId;
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

        public FixtureBuilder winningTeamName(String winningTeamName) {
            this.winningTeamName = winningTeamName;
            return this;
        }

        public FixtureBuilder predictionComment(String predictionComment) {
            this.predictionComment = predictionComment;
            return this;
        }

        public FixtureBuilder iaAnalysis(AnalysisData iaAnalysis) {
            this.iaAnalysis = iaAnalysis;
            return this;
        }

        public Fixture build() {
            return new Fixture(id, date, statusLong, statusShort, leagueName, homeTeam, homeTeamId, awayTeam,
                    awayTeamId, homeTeamGoals, awayTeamGoals, odds, winningTeamName, predictionComment, iaAnalysis);
        }
    }
}
