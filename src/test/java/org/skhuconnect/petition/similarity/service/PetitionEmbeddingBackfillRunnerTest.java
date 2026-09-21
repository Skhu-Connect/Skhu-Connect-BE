package org.skhuconnect.petition.similarity.service;

import org.junit.jupiter.api.Test;
import org.skhuconnect.petition.similarity.config.PetitionEmbeddingBackfillProperties;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class PetitionEmbeddingBackfillRunnerTest {

    @Test
    void runsBackfillLogsSummaryAndClosesContext(CapturedOutput output) {
        PetitionEmbeddingBackfillService backfillService =
                mock(PetitionEmbeddingBackfillService.class);
        PetitionEmbeddingBackfillProperties properties =
                new PetitionEmbeddingBackfillProperties();
        properties.setLimit(4);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(backfillService.backfillPublicPetitions(4))
                .thenReturn(new PetitionEmbeddingBackfillResult(4, 3, 1, 0));
        PetitionEmbeddingBackfillRunner runner = new PetitionEmbeddingBackfillRunner(
                backfillService, properties, context);

        runner.run(new DefaultApplicationArguments());

        verify(backfillService).backfillPublicPetitions(4);
        verify(context).close();
        assertThat(output).contains("petition embedding backfill started: limit=4");
        assertThat(output).contains(
                "petition embedding backfill finished: totalCandidates=4, success=3, failure=1, skipped=0");
    }
}
