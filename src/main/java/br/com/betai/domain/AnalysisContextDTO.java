package br.com.betai.domain;

public class AnalysisContextDTO {
    private Fixture fixture;
    private String statistics;
    private String predictions;

    public AnalysisContextDTO() {
    }

    public AnalysisContextDTO(Fixture fixture, String statistics, String predictions) {
        this.fixture = fixture;
        this.statistics = statistics;
        this.predictions = predictions;
    }

    public Fixture getFixture() {
        return fixture;
    }

    public void setFixture(Fixture fixture) {
        this.fixture = fixture;
    }

    public String getStatistics() {
        return statistics;
    }

    public void setStatistics(String statistics) {
        this.statistics = statistics;
    }

    public String getPredictions() {
        return predictions;
    }

    public void setPredictions(String predictions) {
        this.predictions = predictions;
    }
}
