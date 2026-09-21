package org.skhuconnect.petition.similarity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.petition.similarity")
public class PetitionSimilarityProperties {

    private double threshold = 0.75;
    private int searchLimit = 3;
    private Duration searchWindow = Duration.ofMinutes(10);

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public int getSearchLimit() {
        return searchLimit;
    }

    public void setSearchLimit(int searchLimit) {
        this.searchLimit = searchLimit;
    }

    public Duration getSearchWindow() {
        return searchWindow;
    }

    public void setSearchWindow(Duration searchWindow) {
        this.searchWindow = searchWindow;
    }
}
