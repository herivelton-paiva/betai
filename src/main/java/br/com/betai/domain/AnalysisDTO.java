package br.com.betai.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisDTO {
    private Long fixtureId;
    private String rule;
    private String match;
    private String league;
    private String date;
    private OddsSummary odds;
    private StatsSummary stats;

    public AnalysisDTO() {
    }

    public AnalysisDTO(Long fixtureId, String rule, String match, String league, String date, OddsSummary odds,
            StatsSummary stats) {
        this.fixtureId = fixtureId;
        this.rule = rule;
        this.match = match;
        this.league = league;
        this.date = date;
        this.odds = odds;
        this.stats = stats;
    }

    public static AnalysisDTOBuilder builder() {
        return new AnalysisDTOBuilder();
    }

    public Long getFixtureId() {
        return fixtureId;
    }

    public void setFixtureId(Long fixtureId) {
        this.fixtureId = fixtureId;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public String getLeague() {
        return league;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public OddsSummary getOdds() {
        return odds;
    }

    public void setOdds(OddsSummary odds) {
        this.odds = odds;
    }

    public StatsSummary getStats() {
        return stats;
    }

    public void setStats(StatsSummary stats) {
        this.stats = stats;
    }

    public static class AnalysisDTOBuilder {
        private Long fixtureId;
        private String rule;
        private String match;
        private String league;
        private String date;
        private OddsSummary odds;
        private StatsSummary stats;

        public AnalysisDTOBuilder fixtureId(Long fixtureId) {
            this.fixtureId = fixtureId;
            return this;
        }

        public AnalysisDTOBuilder rule(String rule) {
            this.rule = rule;
            return this;
        }

        public AnalysisDTOBuilder match(String match) { this.match = match; return this; }
        public AnalysisDTOBuilder league(String league) { this.league = league; return this; }

        public AnalysisDTOBuilder date(String date) {
            this.date = date;
            return this;
        }

        public AnalysisDTOBuilder odds(OddsSummary odds) {
            this.odds = odds;
            return this;
        }

        public AnalysisDTOBuilder stats(StatsSummary stats) {
            this.stats = stats;
            return this;
        }

        public AnalysisDTO build() {
            return new AnalysisDTO(fixtureId, rule, match, league, date, odds, stats);
        }
    }

    public static class OddsSummary {
        private String matchWinner;
        private String goalsOverUnder;
        private String bothTeamsToScore;

        public OddsSummary() {
        }

        public OddsSummary(String matchWinner, String goalsOverUnder, String bothTeamsToScore) {
            this.matchWinner = matchWinner;
            this.goalsOverUnder = goalsOverUnder;
            this.bothTeamsToScore = bothTeamsToScore;
        }

        public static OddsSummaryBuilder builder() {
            return new OddsSummaryBuilder();
        }

        public String getMatchWinner() {
            return matchWinner;
        }

        public void setMatchWinner(String matchWinner) {
            this.matchWinner = matchWinner;
        }

        public String getGoalsOverUnder() {
            return goalsOverUnder;
        }

        public void setGoalsOverUnder(String goalsOverUnder) {
            this.goalsOverUnder = goalsOverUnder;
        }

        public String getBothTeamsToScore() {
            return bothTeamsToScore;
        }

        public void setBothTeamsToScore(String bothTeamsToScore) {
            this.bothTeamsToScore = bothTeamsToScore;
        }

        public static class OddsSummaryBuilder {
            private String matchWinner;
            private String goalsOverUnder;
            private String bothTeamsToScore;

            public OddsSummaryBuilder matchWinner(String matchWinner) {
                this.matchWinner = matchWinner;
                return this;
            }

            public OddsSummaryBuilder goalsOverUnder(String goalsOverUnder) {
                this.goalsOverUnder = goalsOverUnder;
                return this;
            }

            public OddsSummaryBuilder bothTeamsToScore(String bothTeamsToScore) {
                this.bothTeamsToScore = bothTeamsToScore;
                return this;
            }

            public OddsSummary build() {
                return new OddsSummary(matchWinner, goalsOverUnder, bothTeamsToScore);
            }
        }
    }

    public static class StatsSummary {
        private TeamAnalysis home;
        private TeamAnalysis away;
        private H2HStats h2h;

        public StatsSummary() {
        }

        public StatsSummary(TeamAnalysis home, TeamAnalysis away, H2HStats h2h) {
            this.home = home;
            this.away = away;
            this.h2h = h2h;
        }

        public static StatsSummaryBuilder builder() {
            return new StatsSummaryBuilder();
        }

        public TeamAnalysis getHome() {
            return home;
        }

        public void setHome(TeamAnalysis home) {
            this.home = home;
        }

        public TeamAnalysis getAway() {
            return away;
        }

        public void setAway(TeamAnalysis away) {
            this.away = away;
        }

        public H2HStats getH2h() {
            return h2h;
        }

        public void setH2h(H2HStats h2h) {
            this.h2h = h2h;
        }

        public static class StatsSummaryBuilder {
            private TeamAnalysis home;
            private TeamAnalysis away;
            private H2HStats h2h;

            public StatsSummaryBuilder home(TeamAnalysis home) {
                this.home = home;
                return this;
            }

            public StatsSummaryBuilder away(TeamAnalysis away) {
                this.away = away;
                return this;
            }

            public StatsSummaryBuilder h2h(H2HStats h2h) {
                this.h2h = h2h;
                return this;
            }

            public StatsSummary build() {
                return new StatsSummary(home, away, h2h);
            }
        }
    }

    public static class TeamAnalysis {
        private String name;
        private String form;
        private Double avgGoalsFor;
        private Double avgGoalsAgainst;
        private Integer cleanSheets;
        private Integer failedToScore;
        private String over25Percentage;
        private Double avgGoalsForAway;
        private String goalsMinutes;

        public TeamAnalysis() {
        }

        public TeamAnalysis(String name, String form, Double avgGoalsFor, Double avgGoalsAgainst, Integer cleanSheets,
                Integer failedToScore, String over25Percentage, Double avgGoalsForAway, String goalsMinutes) {
            this.name = name;
            this.form = form;
            this.avgGoalsFor = avgGoalsFor;
            this.avgGoalsAgainst = avgGoalsAgainst;
            this.cleanSheets = cleanSheets;
            this.failedToScore = failedToScore;
            this.over25Percentage = over25Percentage;
            this.avgGoalsForAway = avgGoalsForAway;
            this.goalsMinutes = goalsMinutes;
        }

        public static TeamAnalysisBuilder builder() {
            return new TeamAnalysisBuilder();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getForm() {
            return form;
        }

        public void setForm(String form) {
            this.form = form;
        }

        public Double getAvgGoalsFor() {
            return avgGoalsFor;
        }

        public void setAvgGoalsFor(Double avgGoalsFor) {
            this.avgGoalsFor = avgGoalsFor;
        }

        public Double getAvgGoalsAgainst() {
            return avgGoalsAgainst;
        }

        public void setAvgGoalsAgainst(Double avgGoalsAgainst) {
            this.avgGoalsAgainst = avgGoalsAgainst;
        }

        public Integer getCleanSheets() {
            return cleanSheets;
        }

        public void setCleanSheets(Integer cleanSheets) {
            this.cleanSheets = cleanSheets;
        }

        public Integer getFailedToScore() {
            return failedToScore;
        }

        public void setFailedToScore(Integer failedToScore) {
            this.failedToScore = failedToScore;
        }

        public String getOver25Percentage() {
            return over25Percentage;
        }

        public void setOver25Percentage(String over25Percentage) {
            this.over25Percentage = over25Percentage;
        }

        public Double getAvgGoalsForAway() {
            return avgGoalsForAway;
        }

        public void setAvgGoalsForAway(Double avgGoalsForAway) {
            this.avgGoalsForAway = avgGoalsForAway;
        }

        public String getGoalsMinutes() {
            return goalsMinutes;
        }

        public void setGoalsMinutes(String goalsMinutes) {
            this.goalsMinutes = goalsMinutes;
        }

        public static class TeamAnalysisBuilder {
            private String name;
            private String form;
            private Double avgGoalsFor;
            private Double avgGoalsAgainst;
            private Integer cleanSheets;
            private Integer failedToScore;
            private String over25Percentage;
            private Double avgGoalsForAway;
            private String goalsMinutes;

            public TeamAnalysisBuilder name(String name) {
                this.name = name;
                return this;
            }

            public TeamAnalysisBuilder form(String form) {
                this.form = form;
                return this;
            }

            public TeamAnalysisBuilder avgGoalsFor(Double avgGoalsFor) {
                this.avgGoalsFor = avgGoalsFor;
                return this;
            }

            public TeamAnalysisBuilder avgGoalsAgainst(Double avgGoalsAgainst) {
                this.avgGoalsAgainst = avgGoalsAgainst;
                return this;
            }

            public TeamAnalysisBuilder cleanSheets(Integer cleanSheets) {
                this.cleanSheets = cleanSheets;
                return this;
            }

            public TeamAnalysisBuilder failedToScore(Integer failedToScore) {
                this.failedToScore = failedToScore;
                return this;
            }

            public TeamAnalysisBuilder over25Percentage(String over25Percentage) {
                this.over25Percentage = over25Percentage;
                return this;
            }

            public TeamAnalysisBuilder avgGoalsForAway(Double avgGoalsForAway) {
                this.avgGoalsForAway = avgGoalsForAway;
                return this;
            }

            public TeamAnalysisBuilder goalsMinutes(String goalsMinutes) {
                this.goalsMinutes = goalsMinutes;
                return this;
            }

            public TeamAnalysis build() {
                return new TeamAnalysis(name, form, avgGoalsFor, avgGoalsAgainst, cleanSheets, failedToScore,
                        over25Percentage, avgGoalsForAway, goalsMinutes);
            }
        }
    }

    public static class H2HStats {
        private String summary;

        public H2HStats() {
        }

        public H2HStats(String summary) {
            this.summary = summary;
        }

        public static H2HStatsBuilder builder() {
            return new H2HStatsBuilder();
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public static class H2HStatsBuilder {
            private String summary;

            public H2HStatsBuilder summary(String summary) {
                this.summary = summary;
                return this;
            }

            public H2HStats build() {
                return new H2HStats(summary);
            }
        }
    }
}
