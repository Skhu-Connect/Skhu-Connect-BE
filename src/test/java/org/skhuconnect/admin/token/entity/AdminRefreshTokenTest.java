package org.skhuconnect.admin.token.entity;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AdminRefreshTokenTest {

    @Test
    void mapsSeparateTokenTableAndUniqueIndexes() {
        Table table = AdminRefreshToken.class.getAnnotation(Table.class);
        assertThat(table.name()).isEqualTo("admin_refresh_tokens");
        assertThat(Arrays.stream(table.indexes()).map(Index::name))
                .contains("ux_admin_refresh_tokens_admin_id", "ux_admin_refresh_tokens_token_hash");
    }

    @Test
    void expiryBoundaryAndRotationAreExplicit() {
        LocalDateTime expiry = LocalDateTime.of(2030, 1, 1, 0, 0);
        AdminRefreshToken token = AdminRefreshToken.create(
                org.mockito.Mockito.mock(org.skhuconnect.admin.entity.Admin.class),
                "a".repeat(64), expiry);
        assertThat(token.isExpiredAt(expiry.minusNanos(1))).isFalse();
        assertThat(token.isExpiredAt(expiry)).isTrue();
        token.rotate("b".repeat(64), expiry.plusDays(1));
        assertThat(token.getTokenHash()).isEqualTo("b".repeat(64));
    }
}