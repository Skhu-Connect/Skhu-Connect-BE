package org.skhuconnect.auth.email.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.skhuconnect.global.entity.BaseEntity;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTest {

    @Test
    void hasDesignedJpaMapping() throws Exception {
        assertThat(EmailVerification.class.getSuperclass()).isEqualTo(BaseEntity.class);
        Table table = EmailVerification.class.getAnnotation(Table.class);
        assertThat(table.name()).isEqualTo("email_verifications");
        assertThat(Arrays.stream(table.indexes()).map(Index::name))
                .containsExactlyInAnyOrder(
                        "ux_email_verifications_email_purpose",
                        "ux_email_verifications_token_hash");

        assertColumn("email", "email", 255, false);
        assertColumn("codeHash", "code_hash", 64, false);
        assertColumn("codeSalt", "code_salt", 64, false);
        assertColumn("tokenHash", "token_hash", 64, true);
        Enumerated enumerated = EmailVerification.class
                .getDeclaredField("purpose").getAnnotation(Enumerated.class);
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        JdbcTypeCode jdbcTypeCode = EmailVerification.class
                .getDeclaredField("purpose").getAnnotation(JdbcTypeCode.class);
        assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.VARCHAR);
    }

    @Test
    void refreshFailureVerificationAndConsumptionFollowPolicy() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 4, 12, 0);
        EmailVerification verification = EmailVerification.create(
                "student@office.skhu.ac.kr", EmailVerificationPurpose.SIGN_UP,
                "hash", "salt", now.plusMinutes(5), now);

        assertThat(verification.canResendAt(now.plusSeconds(59))).isFalse();
        assertThat(verification.canResendAt(now.plusSeconds(60))).isTrue();
        assertThat(verification.isCodeExpiredAt(now.plusMinutes(5))).isTrue();

        for (int count = 0; count < 5; count++) {
            verification.increaseAttemptCount();
        }
        assertThat(verification.hasReachedAttemptLimit()).isTrue();

        verification.refreshCode("new-hash", "new-salt",
                now.plusMinutes(6), now.plusMinutes(1));
        assertThat(verification.getAttemptCount()).isZero();
        assertThat(verification.getTokenHash()).isNull();

        verification.verify("token-hash", now, now.plusMinutes(30));
        assertThat(verification.isVerified()).isTrue();
        assertThat(verification.isTokenExpiredAt(now.plusMinutes(30))).isTrue();
        verification.consume(now.plusMinutes(1));
        assertThat(verification.isUsed()).isTrue();
    }

    @Test
    void hasNoSetter() {
        assertThat(Arrays.stream(EmailVerification.class.getMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))).isEmpty();
    }

    private void assertColumn(
            String fieldName, String columnName, int length, boolean nullable)
            throws Exception {
        Column column = EmailVerification.class.getDeclaredField(fieldName)
                .getAnnotation(Column.class);
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.length()).isEqualTo(length);
        assertThat(column.nullable()).isEqualTo(nullable);
    }
}
