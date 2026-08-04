package org.skhuconnect.auth.token.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.skhuconnect.global.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void mapsRequiredColumnsAndUniqueIndexes() throws Exception {
        assertThat(RefreshToken.class.getSuperclass()).isEqualTo(BaseEntity.class);
        Table table = RefreshToken.class.getAnnotation(Table.class);
        assertThat(table.name()).isEqualTo("refresh_tokens");
        assertThat(Arrays.stream(table.indexes()).map(Index::name))
                .contains("ux_refresh_tokens_user_id", "ux_refresh_tokens_token_hash");
        assertThat(Arrays.stream(table.indexes()).allMatch(Index::unique)).isTrue();

        JoinColumn user = RefreshToken.class.getDeclaredField("user")
                .getAnnotation(JoinColumn.class);
        assertThat(user.name()).isEqualTo("user_id");
        assertThat(user.nullable()).isFalse();
        assertThat(user.unique()).isTrue();

        Column hash = RefreshToken.class.getDeclaredField("tokenHash")
                .getAnnotation(Column.class);
        assertThat(hash.name()).isEqualTo("token_hash");
        assertThat(hash.length()).isEqualTo(64);
        assertThat(hash.nullable()).isFalse();
        assertThat(hash.unique()).isTrue();
    }

    @Test
    void expiryBoundaryAndRotationAreExplicit() {
        LocalDateTime expiry = LocalDateTime.of(2030, 1, 1, 0, 0);
        RefreshToken token = RefreshToken.create(
                org.mockito.Mockito.mock(org.skhuconnect.user.entity.User.class),
                "a".repeat(64), expiry);
        assertThat(token.isExpiredAt(expiry.minusNanos(1))).isFalse();
        assertThat(token.isExpiredAt(expiry)).isTrue();

        token.rotate("b".repeat(64), expiry.plusDays(1));
        assertThat(token.getTokenHash()).isEqualTo("b".repeat(64));
        assertThat(token.getExpiresAt()).isEqualTo(expiry.plusDays(1));
    }
}