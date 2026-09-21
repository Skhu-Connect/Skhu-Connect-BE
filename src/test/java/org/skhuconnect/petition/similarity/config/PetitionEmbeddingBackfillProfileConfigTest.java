package org.skhuconnect.petition.similarity.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class PetitionEmbeddingBackfillProfileConfigTest {

    @Test
    void embeddingBackfillProfileDisablesWebServer() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-embedding-backfill.yml"));

        assertThat(factory.getObject())
                .containsEntry("spring.main.web-application-type", "none");
    }
}
