package org.skhuconnect.petition.similarity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("embedding-backfill")
@ConfigurationProperties(prefix = "app.petition.embedding-backfill")
public class PetitionEmbeddingBackfillProperties {

    private int limit = 1000;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
