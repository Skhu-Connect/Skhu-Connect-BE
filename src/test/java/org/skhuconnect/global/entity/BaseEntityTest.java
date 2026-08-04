package org.skhuconnect.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Test;
import org.skhuconnect.global.config.JpaAuditingConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    void baseEntityHasJpaAuditingAnnotations() {
        assertThat(BaseEntity.class).hasAnnotation(MappedSuperclass.class);
        assertThat(BaseEntity.class.getAnnotation(EntityListeners.class).value())
                .containsExactly(AuditingEntityListener.class);
    }

    @Test
    void baseEntityHasDesignedAuditFields() throws NoSuchFieldException {
        Field createdAt = BaseEntity.class.getDeclaredField("createdAt");
        Field updatedAt = BaseEntity.class.getDeclaredField("updatedAt");

        assertThat(createdAt.getType()).isEqualTo(LocalDateTime.class);
        assertThat(createdAt.getAnnotation(CreatedDate.class)).isNotNull();
        assertColumn(createdAt, "created_at", false);

        assertThat(updatedAt.getType()).isEqualTo(LocalDateTime.class);
        assertThat(updatedAt.getAnnotation(LastModifiedDate.class)).isNotNull();
        assertColumn(updatedAt, "updated_at", true);
    }

    @Test
    void jpaAuditingIsEnabled() {
        assertThat(JpaAuditingConfig.class).hasAnnotation(Configuration.class);
        assertThat(JpaAuditingConfig.class).hasAnnotation(EnableJpaAuditing.class);
    }

    private void assertColumn(Field field, String name, boolean updatable) {
        Column column = field.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo(name);
        assertThat(column.nullable()).isFalse();
        assertThat(column.updatable()).isEqualTo(updatable);
    }
}

