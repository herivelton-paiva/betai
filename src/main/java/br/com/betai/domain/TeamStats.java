package br.com.betai.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamStats {

    private Fixtures fixtures;
    private Goals goals;
    private String form;
    @JsonProperty("clean_sheet")
    private StatsTotal cleanSheet;
    @JsonProperty("failed_to_score")
    private StatsTotal failedToScore;
    @JsonProperty("biggest")
    private Biggest biggest;

    public TeamStats() {
    }

    public TeamStats(Fixtures fixtures, Goals goals, String form, StatsTotal cleanSheet, StatsTotal failedToScore,
            Biggest biggest) {
        this.fixtures = fixtures;
        this.goals = goals;
        this.form = form;
        this.cleanSheet = cleanSheet;
        this.failedToScore = failedToScore;
        this.biggest = biggest;
    }

    public static TeamStatsBuilder builder() {
        return new TeamStatsBuilder();
    }

    // Getters and Setters
    public Fixtures getFixtures() {
        return fixtures;
    }

    public void setFixtures(Fixtures fixtures) {
        this.fixtures = fixtures;
    }

    public Goals getGoals() {
        return goals;
    }

    public void setGoals(Goals goals) {
        this.goals = goals;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public StatsTotal getCleanSheet() {
        return cleanSheet;
    }

    public void setCleanSheet(StatsTotal cleanSheet) {
        this.cleanSheet = cleanSheet;
    }

    public StatsTotal getFailedToScore() {
        return failedToScore;
    }

    public void setFailedToScore(StatsTotal failedToScore) {
        this.failedToScore = failedToScore;
    }

    public Biggest getBiggest() {
        return biggest;
    }

    public void setBiggest(Biggest biggest) {
        this.biggest = biggest;
    }

    // Métodos utilitários
    public double getUnder25Rate() {
        if (fixtures == null || fixtures.getPlayed() == null || fixtures.getPlayed().getTotal() == 0 || goals == null
                || goals.getAgainst() == null || goals.getAgainst().getUnderOver() == null
                || !goals.getAgainst().getUnderOver().containsKey("2.5")) {
            return 0.0;
        }
        int totalPlayed = fixtures.getPlayed().getTotal();
        int under25 = goals.getAgainst().getUnderOver().get("2.5").getUnder();
        return (double) under25 / totalPlayed;
    }

    public int getHomeWins() {
        return (fixtures != null && fixtures.getWins() != null) ? fixtures.getWins().getHome() : 0;
    }

    public int getGamesPlayed() {
        return (fixtures != null && fixtures.getPlayed() != null) ? fixtures.getPlayed().getTotal() : 0;
    }

    public double getAvgGoalsAgainst() {
        if (goals == null || goals.getAgainst() == null || goals.getAgainst().getAverage() == null
                || goals.getAgainst().getAverage().getTotal() == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(goals.getAgainst().getAverage().getTotal());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public double getFailedToScoreRate() {
        if (fixtures == null || fixtures.getPlayed() == null || fixtures.getPlayed().getTotal() == 0
                || failedToScore == null) {
            return 0.0;
        }
        return (double) failedToScore.getTotal() / fixtures.getPlayed().getTotal();
    }

    public double getLateGoalsAgainstPct() {
        if (goals == null || goals.getAgainst() == null || goals.getAgainst().getMinute() == null) {
            return 0.0;
        }
        int totalGoalsAgainst = (goals.getAgainst().getTotal() != null) ? goals.getAgainst().getTotal().getTotal() : 0;
        if (totalGoalsAgainst == 0)
            return 0.0;
        int lateGoals = 0;
        if (goals.getAgainst().getMinute().containsKey("76-90")) {
            lateGoals += goals.getAgainst().getMinute().get("76-90").getTotal();
        }
        if (goals.getAgainst().getMinute().containsKey("91-105")) {
            lateGoals += goals.getAgainst().getMinute().get("91-105").getTotal();
        }
        return (double) lateGoals / totalGoalsAgainst;
    }

    public int getCleanSheets() {
        return (cleanSheet != null) ? cleanSheet.getTotal() : 0;
    }

    public double getAvgGoalsFor() {
        if (goals == null || goals.getGoalsFor() == null || goals.getGoalsFor().getAverage() == null
                || goals.getGoalsFor().getAverage().getTotal() == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(goals.getGoalsFor().getAverage().getTotal());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public int getPlayedAway() {
        return (fixtures != null && fixtures.getPlayed() != null) ? fixtures.getPlayed().getAway() : 0;
    }

    public int getFailedToScoreAway() {
        return (failedToScore != null) ? failedToScore.getAway() : 0;
    }

    public String getAvgGoalsForAway() {
        if (goals == null || goals.getGoalsFor() == null || goals.getGoalsFor().getAverage() == null) {
            return "0.0";
        }
        return goals.getGoalsFor().getAverage().getAway();
    }

    public String getBiggestWinAway() {
        if (biggest == null || biggest.getWins() == null)
            return null;
        return biggest.getWins().getAway();
    }

    public String getBiggestLossAway() {
        if (biggest == null || biggest.getLoses() == null)
            return null;
        return biggest.getLoses().getAway();
    }

    public String getGoalsMinutesDistribution() {
        if (goals == null || goals.getGoalsFor() == null || goals.getGoalsFor().getMinute() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        goals.getGoalsFor().getMinute().forEach((k, v) -> {
            if (v.getTotal() > 0) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(k).append(": ").append(v.getPercentage());
            }
        });
        return sb.toString();
    }

    // Builder
    public static class TeamStatsBuilder {
        private Fixtures fixtures;
        private Goals goals;
        private String form;
        private StatsTotal cleanSheet;
        private StatsTotal failedToScore;
        private Biggest biggest;

        public TeamStatsBuilder fixtures(Fixtures fixtures) {
            this.fixtures = fixtures;
            return this;
        }

        public TeamStatsBuilder goals(Goals goals) {
            this.goals = goals;
            return this;
        }

        public TeamStatsBuilder form(String form) {
            this.form = form;
            return this;
        }

        public TeamStatsBuilder cleanSheet(StatsTotal cleanSheet) {
            this.cleanSheet = cleanSheet;
            return this;
        }

        public TeamStatsBuilder failedToScore(StatsTotal failedToScore) {
            this.failedToScore = failedToScore;
            return this;
        }

        public TeamStatsBuilder biggest(Biggest biggest) {
            this.biggest = biggest;
            return this;
        }

        public TeamStats build() {
            return new TeamStats(fixtures, goals, form, cleanSheet, failedToScore, biggest);
        }
    }

    // Inner Classes (Manual)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fixtures {
        private StatsDetail played;
        private StatsDetail wins;
        private StatsDetail draws;
        private StatsDetail loses;

        public StatsDetail getPlayed() {
            return played;
        }

        public void setPlayed(StatsDetail played) {
            this.played = played;
        }

        public StatsDetail getWins() {
            return wins;
        }

        public void setWins(StatsDetail wins) {
            this.wins = wins;
        }

        public StatsDetail getDraws() {
            return draws;
        }

        public void setDraws(StatsDetail draws) {
            this.draws = draws;
        }

        public StatsDetail getLoses() {
            return loses;
        }

        public void setLoses(StatsDetail loses) {
            this.loses = loses;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatsDetail {
        private int home;
        private int away;
        private int total;

        public int getHome() {
            return home;
        }

        public void setHome(int home) {
            this.home = home;
        }

        public int getAway() {
            return away;
        }

        public void setAway(int away) {
            this.away = away;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Goals {
        @JsonProperty("for")
        private GoalsDetail goalsFor;
        @JsonProperty("against")
        private GoalsDetail against;

        public GoalsDetail getGoalsFor() {
            return goalsFor;
        }

        public void setGoalsFor(GoalsDetail goalsFor) {
            this.goalsFor = goalsFor;
        }

        public GoalsDetail getAgainst() {
            return against;
        }

        public void setAgainst(GoalsDetail against) {
            this.against = against;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoalsDetail {
        private StatsDetail total;
        private Average average;
        private Map<String, Minute> minute;
        @JsonProperty("under_over")
        private Map<String, UnderOver> underOver;

        public StatsDetail getTotal() {
            return total;
        }

        public void setTotal(StatsDetail total) {
            this.total = total;
        }

        public Average getAverage() {
            return average;
        }

        public void setAverage(Average average) {
            this.average = average;
        }

        public Map<String, Minute> getMinute() {
            return minute;
        }

        public void setMinute(Map<String, Minute> minute) {
            this.minute = minute;
        }

        public Map<String, UnderOver> getUnderOver() {
            return underOver;
        }

        public void setUnderOver(Map<String, UnderOver> underOver) {
            this.underOver = underOver;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Average {
        private String home;
        private String away;
        private String total;

        public String getHome() {
            return home;
        }

        public void setHome(String home) {
            this.home = home;
        }

        public String getAway() {
            return away;
        }

        public void setAway(String away) {
            this.away = away;
        }

        public String getTotal() {
            return total;
        }

        public void setTotal(String total) {
            this.total = total;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Minute {
        private String percentage;
        private int total;

        public String getPercentage() {
            return percentage;
        }

        public void setPercentage(String percentage) {
            this.percentage = percentage;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UnderOver {
        private int over;
        private int under;

        public int getOver() {
            return over;
        }

        public void setOver(int over) {
            this.over = over;
        }

        public int getUnder() {
            return under;
        }

        public void setUnder(int under) {
            this.under = under;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatsTotal {
        private int home;
        private int away;
        private int total;

        public int getHome() {
            return home;
        }

        public void setHome(int home) {
            this.home = home;
        }

        public int getAway() {
            return away;
        }

        public void setAway(int away) {
            this.away = away;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Biggest {
        private BiggestScore wins;
        private BiggestScore loses;

        public BiggestScore getWins() {
            return wins;
        }

        public void setWins(BiggestScore wins) {
            this.wins = wins;
        }

        public BiggestScore getLoses() {
            return loses;
        }

        public void setLoses(BiggestScore loses) {
            this.loses = loses;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BiggestScore {
        private String home;
        private String away;

        public String getHome() {
            return home;
        }

        public void setHome(String home) {
            this.home = home;
        }

        public String getAway() {
            return away;
        }

        public void setAway(String away) {
            this.away = away;
        }
    }
}
