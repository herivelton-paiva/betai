package br.com.betai.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisData {
    private FixtureData fixture;

    @JsonProperty("bet_suggestion")
    private BetSuggestion betSuggestion;

    @JsonProperty("goals_market")
    private GoalsMarket goalsMarket;

    @JsonProperty("probabilities")
    private Probabilities probabilities;

    @JsonProperty("prediction")
    private CorrectScorePrediction prediction;

    @JsonProperty("winner")
    private WinnerNode winner;

    @JsonProperty("win_or_draw")
    private boolean winOrDraw;

    public FixtureData getFixture() {
        return fixture;
    }

    public void setFixture(FixtureData fixture) {
        this.fixture = fixture;
    }

    public BetSuggestion getBetSuggestion() {
        return betSuggestion;
    }

    public void setBetSuggestion(BetSuggestion betSuggestion) {
        this.betSuggestion = betSuggestion;
    }

    public GoalsMarket getGoalsMarket() {
        return goalsMarket;
    }

    public void setGoalsMarket(GoalsMarket goalsMarket) {
        this.goalsMarket = goalsMarket;
    }

    public Probabilities getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(Probabilities probabilities) {
        this.probabilities = probabilities;
    }

    public CorrectScorePrediction getPrediction() {
        return prediction;
    }

    public void setPrediction(CorrectScorePrediction prediction) {
        this.prediction = prediction;
    }

    public WinnerNode getWinner() {
        return winner;
    }

    public void setWinner(WinnerNode winner) {
        this.winner = winner;
    }

    public boolean isWinOrDraw() {
        return winOrDraw;
    }

    public void setWinOrDraw(boolean winOrDraw) {
        this.winOrDraw = winOrDraw;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FixtureData {
        private Long id;
        private TeamsData teams;
        private String date;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public TeamsData getTeams() {
            return teams;
        }

        public void setTeams(TeamsData teams) {
            this.teams = teams;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamsData {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BetSuggestion {
        private String market;
        @JsonProperty("odd_bookmaker")
        private double oddBookmaker;
        @JsonProperty("odd_fair_poisson")
        private double oddFairPoisson;
        @JsonProperty("probability_ai")
        private double probabilityAi;
        @JsonProperty("expected_value")
        private double expectedValue;
        @JsonProperty("status_ev")
        private String statusEv;
        private String justification;

        public String getMarket() {
            return market;
        }

        public void setMarket(String market) {
            this.market = market;
        }

        public double getOddBookmaker() {
            return oddBookmaker;
        }

        public void setOddBookmaker(double oddBookmaker) {
            this.oddBookmaker = oddBookmaker;
        }

        public double getOddFairPoisson() {
            return oddFairPoisson;
        }

        public void setOddFairPoisson(double oddFairPoisson) {
            this.oddFairPoisson = oddFairPoisson;
        }

        public double getProbabilityAi() {
            return probabilityAi;
        }

        public void setProbabilityAi(double probabilityAi) {
            this.probabilityAi = probabilityAi;
        }

        public double getExpectedValue() {
            return expectedValue;
        }

        public void setExpectedValue(double expectedValue) {
            this.expectedValue = expectedValue;
        }

        public String getStatusEv() {
            return statusEv;
        }

        public void setStatusEv(String statusEv) {
            this.statusEv = statusEv;
        }

        public String getJustification() {
            return justification;
        }

        public void setJustification(String justification) {
            this.justification = justification;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoalsMarket {
        private String target;
        private double odd;

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public double getOdd() {
            return odd;
        }

        public void setOdd(double odd) {
            this.odd = odd;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Probabilities {
        @JsonProperty("home_win")
        private double homeWin;
        private double draw;
        @JsonProperty("away_win")
        private double awayWin;
        @JsonProperty("confidence_level")
        private String confidenceLevel;

        public double getHomeWin() {
            return homeWin;
        }

        public void setHomeWin(double homeWin) {
            this.homeWin = homeWin;
        }

        public double getDraw() {
            return draw;
        }

        public void setDraw(double draw) {
            this.draw = draw;
        }

        public double getAwayWin() {
            return awayWin;
        }

        public void setAwayWin(double awayWin) {
            this.awayWin = awayWin;
        }

        public String getConfidenceLevel() {
            return confidenceLevel;
        }

        public void setConfidenceLevel(String confidenceLevel) {
            this.confidenceLevel = confidenceLevel;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CorrectScorePrediction {
        @JsonProperty("correct_score")
        private String correctScore;
        @JsonProperty("score_odd")
        private double scoreOdd;

        public String getCorrectScore() {
            return correctScore;
        }

        public void setCorrectScore(String correctScore) {
            this.correctScore = correctScore;
        }

        public double getScoreOdd() {
            return scoreOdd;
        }

        public void setScoreOdd(double scoreOdd) {
            this.scoreOdd = scoreOdd;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WinnerNode {
        private Long id;
        private String name;

        public WinnerNode() {
        }

        public WinnerNode(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
