package org.skhuconnect.petition.similarity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skhuconnect.petition.similarity.config.PetitionEmbeddingBackfillProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("embedding-backfill")
public class PetitionEmbeddingBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(
            PetitionEmbeddingBackfillRunner.class);

    private final PetitionEmbeddingBackfillService backfillService;
    private final PetitionEmbeddingBackfillProperties properties;
    private final ConfigurableApplicationContext context;

    public PetitionEmbeddingBackfillRunner(
            PetitionEmbeddingBackfillService backfillService,
            PetitionEmbeddingBackfillProperties properties,
            ConfigurableApplicationContext context
    ) {
        this.backfillService = backfillService;
        this.properties = properties;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("petition embedding backfill started: limit={}", properties.getLimit());
        PetitionEmbeddingBackfillResult result = backfillService.backfillPublicPetitions(
                properties.getLimit());
        log.info(
                "petition embedding backfill finished: totalCandidates={}, success={}, failure={}, skipped={}",
                result.totalCandidates(),
                result.successCount(),
                result.failureCount(),
                result.skippedCount()
        );
        context.close();
    }
}
