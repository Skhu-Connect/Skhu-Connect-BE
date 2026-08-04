package org.skhuconnect.user.repository;

import org.junit.jupiter.api.Test;
import org.skhuconnect.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest {

    @Test
    void userRepositoryHasRequiredContracts() throws NoSuchMethodException {
        assertThat(JpaRepository.class).isAssignableFrom(UserRepository.class);

        Method existsByEmail = UserRepository.class.getMethod(
                "existsByEmail", String.class
        );
        Method existsByLoginId = UserRepository.class.getMethod(
                "existsByLoginId", String.class
        );
        Method findByLoginId = UserRepository.class.getMethod(
                "findByLoginId", String.class
        );

        assertThat(existsByEmail.getReturnType()).isEqualTo(boolean.class);
        assertThat(existsByLoginId.getReturnType()).isEqualTo(boolean.class);
        assertThat(findByLoginId.getReturnType()).isEqualTo(Optional.class);
        assertThat(findByLoginId.getGenericReturnType().getTypeName())
                .isEqualTo(Optional.class.getName() + "<" + User.class.getName() + ">");
    }
}
