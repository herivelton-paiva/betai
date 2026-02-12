package br.com.betai.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MultiBetResponse {

    private List<MultipleSuggestion> multiples;

    public List<MultipleSuggestion> getMultiples() {
        return multiples;
    }

    public void setMultiples(List<MultipleSuggestion> multiples) {
        this.multiples = multiples;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MultipleSuggestion {
        private String title;
        private int size;
        private List<BetLeg> legs;

        @JsonProperty("final_odd")
        private double finalOdd;

        @JsonProperty("total_probability")
        private double totalProbability;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public List<BetLeg> getLegs() {
            return legs;
        }

        public void setLegs(List<BetLeg> legs) {
            this.legs = legs;
        }

        public double getFinalOdd() {
            return finalOdd;
        }

        public void setFinalOdd(double finalOdd) {
            this.finalOdd = finalOdd;
        }

        public double getTotalProbability() {
            return totalProbability;
        }

        public void setTotalProbability(double totalProbability) {
            this.totalProbability = totalProbability;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BetLeg {
        @JsonProperty("id_fixture")
        private Long idFixture;

        @JsonProperty("team_a")
        private String teamA;

        @JsonProperty("team_b")
        private String teamB;

        @JsonProperty("game_date")
        private String gameDate;

        private String market;
        private double odd;

        public Long getIdFixture() {
            return idFixture;
        }

        public void setIdFixture(Long idFixture) {
            this.idFixture = idFixture;
        }

        public String getTeamA() {
            return teamA;
        }

        public void setTeamA(String teamA) {
            this.teamA = teamA;
        }

        public String getTeamB() {
            return teamB;
        }

        public void setTeamB(String teamB) {
            this.teamB = teamB;
        }

        public String getGameDate() {
            return gameDate;
        }

        public void setGameDate(String gameDate) {
            this.gameDate = gameDate;
        }

        public String getMarket() {
            return market;
        }

        public void setMarket(String market) {
            this.market = market;
        }

        public double getOdd() {
            return odd;
        }

        public void setOdd(double odd) {
            this.odd = odd;
        }
    }
}
