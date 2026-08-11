package org.skhuconnect.auth.signup.entity;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.user.entity.User;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class UserTermsAgreementTest {

    @Test
    void createStoresUserVersionAndServerTime() {
        User user = user();
        LocalDateTime agreedAt = LocalDateTime.of(2030, 1, 1, 0, 0);

        UserTermsAgreement agreement =
                UserTermsAgreement.create(user, "1.0", agreedAt);

        assertThat(agreement.getUser()).isSameAs(user);
        assertThat(agreement.getTermsVersion()).isEqualTo("1.0");
        assertThat(agreement.getAgreedAt()).isEqualTo(agreedAt);
    }

    @Test
    void createRejectsNullValues() {
        User user = user();
        LocalDateTime agreedAt = LocalDateTime.of(2030, 1, 1, 0, 0);

        assertThatNullPointerException().isThrownBy(
                () -> UserTermsAgreement.create(null, "1.0", agreedAt));
        assertThatNullPointerException().isThrownBy(
                () -> UserTermsAgreement.create(user, null, agreedAt));
        assertThatNullPointerException().isThrownBy(
                () -> UserTermsAgreement.create(user, "1.0", null));
    }

    @Test
    void mappingUsesLazyUserWithoutCascadeAndUniqueUserVersion() throws Exception {
        Field userField = UserTermsAgreement.class.getDeclaredField("user");
        ManyToOne manyToOne = userField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = userField.getAnnotation(JoinColumn.class);
        Table table = UserTermsAgreement.class.getAnnotation(Table.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(manyToOne.cascade()).isEmpty();
        assertThat(joinColumn.name()).isEqualTo("user_id");
        assertThat(joinColumn.nullable()).isFalse();
        assertThat(table.uniqueConstraints()[0].columnNames())
                .containsExactly("user_id", "terms_version");
    }

    private User user() {
        return User.create(
                "student@office.skhu.ac.kr",
                "student",
                "encoded-password",
                Department.create("CS", "소프트웨어공학과")
        );
    }
}
