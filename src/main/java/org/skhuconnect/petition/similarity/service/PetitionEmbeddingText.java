package org.skhuconnect.petition.similarity.service;

import org.skhuconnect.petition.entity.Petition;

public final class PetitionEmbeddingText {

    private PetitionEmbeddingText() {
    }

    public static String from(String title, String content) {
        return title.trim() + "\n\n" + content.trim();
    }

    public static String from(Petition petition) {
        return from(petition.getTitle(), petition.getContent());
    }
}
