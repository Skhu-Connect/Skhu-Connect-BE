package org.skhuconnect.auth.email.service;

import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.email.exception.EmailVerificationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailVerificationSupportTest {

    private final EmailNormalizer normalizer = new EmailNormalizer();
    private final VerificationHasher hasher = new VerificationHasher();

    @Test
    void normalizesSchoolEmail() {
        assertThat(normalizer.normalize("  STUDENT@OFFICE.SKHU.AC.KR  "))
                .isEqualTo("student@office.skhu.ac.kr");
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> normalizer.normalize("student@example.com"))
                .isInstanceOf(EmailVerificationException.class);
        assertThatThrownBy(() -> normalizer.normalize(" "))
                .isInstanceOf(EmailVerificationException.class);
        assertThatThrownBy(() -> normalizer.normalize(null))
                .isInstanceOf(EmailVerificationException.class);
    }

    @Test
    void generatedCodesAlwaysHaveSixDigitsAndCanRepresentLeadingZero() {
        VerificationCodeGenerator generator = new VerificationCodeGenerator();
        assertThat(VerificationCodeGenerator.format(0)).isEqualTo("000000");
        for (int count = 0; count < 100; count++) {
            assertThat(generator.generate()).matches("\\d{6}");
        }
    }

    @Test
    void hashesUseSaltAndDoNotContainRawValues() {
        String first = hasher.hashCode("salt-one", "123456");
        assertThat(first).isEqualTo(hasher.hashCode("salt-one", "123456"));
        assertThat(first).isNotEqualTo(hasher.hashCode("salt-two", "123456"));
        assertThat(first).isNotEqualTo(hasher.hashCode("salt-one", "654321"));
        assertThat(first).doesNotContain("123456");

        String tokenHash = hasher.hashToken("raw-token");
        assertThat(tokenHash).hasSize(64).doesNotContain("raw-token");
    }
}
