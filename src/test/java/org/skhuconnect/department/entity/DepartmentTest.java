package org.skhuconnect.department.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.skhuconnect.global.entity.BaseEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentTest {

    @Test
    void departmentHasDesignedJpaMapping() throws NoSuchFieldException {
        Table table = Department.class.getAnnotation(Table.class);
        Field id = Department.class.getDeclaredField("id");
        Field code = Department.class.getDeclaredField("code");
        Field name = Department.class.getDeclaredField("name");

        assertThat(Department.class).hasAnnotation(Entity.class);
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("departments");
        assertThat(Department.class.getSuperclass()).isEqualTo(BaseEntity.class);

        assertThat(id.getType()).isEqualTo(Long.class);
        assertThat(id.getAnnotation(Id.class)).isNotNull();
        assertThat(id.getAnnotation(GeneratedValue.class).strategy())
                .isEqualTo(GenerationType.IDENTITY);

        assertColumn(code, String.class, 50);
        assertColumn(name, String.class, 100);
    }

    @Test
    void departmentHasNoSetter() {
        assertThat(Arrays.stream(Department.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("set")))
                .isEmpty();
    }

    @Test
    void noArgsConstructorIsProtected() throws NoSuchMethodException {
        assertThat(Modifier.isProtected(
                Department.class.getDeclaredConstructor().getModifiers()
        )).isTrue();
    }

    private void assertColumn(Field field, Class<?> type, int length) {
        Column column = field.getAnnotation(Column.class);

        assertThat(field.getType()).isEqualTo(type);
        assertThat(column).isNotNull();
        assertThat(column.length()).isEqualTo(length);
        assertThat(column.nullable()).isFalse();
        assertThat(column.unique()).isTrue();
    }
}
