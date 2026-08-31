package org.skhuconnect.auth.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.password.dto.PasswordResetRequest;
import org.skhuconnect.auth.signup.dto.SignupRequest;
import org.skhuconnect.user.dto.LoginIdUpdateRequest;
import org.skhuconnect.user.dto.PasswordChangeRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthValidationPolicyTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void signupAcceptsAllowedLoginIdAndPasswordPolicy() {
        SignupRequest request = new SignupRequest(
                "token",
                "Student_01.2",
                "password1",
                1L,
                true,
                "1.0"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void signupRejectsInvalidLoginIds() {
        for (String loginId : List.of(
                "abcd",
                "abcdefghijklmnopqrstu",
                "student 01",
                "학생12345",
                "student!"
        )) {
            SignupRequest request = new SignupRequest(
                    "token", loginId, "password1", 1L, true, "1.0");

            assertThat(validator.validate(request))
                    .anyMatch(violation -> violation.getPropertyPath()
                            .toString().equals("loginId"));
        }
    }

    @Test
    void signupRejectsInvalidPasswords() {
        for (String password : List.of(
                "abcde",
                "12345",
                "abc 12",
                "비번abc1",
                "abc-12",
                "a1",
                "abcde12345abcde123456"
        )) {
            SignupRequest request = new SignupRequest(
                    "token", "student01", password, 1L, true, "1.0");

            assertThat(validator.validate(request))
                    .anyMatch(violation -> violation.getPropertyPath()
                            .toString().equals("password"));
        }
    }

    @Test
    void passwordResetAndChangeUseSameNewPasswordPolicy() {
        assertThat(validator.validate(new PasswordResetRequest("token", "newPassword1")))
                .isEmpty();
        assertThat(validator.validate(
                new PasswordChangeRequest("current-password", "newPassword1")))
                .isEmpty();

        assertThat(validator.validate(new PasswordResetRequest("token", "abc-12")))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString().equals("newPassword"));
        assertThat(validator.validate(
                new PasswordChangeRequest("current-password", "abc-12")))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString().equals("newPassword"));
    }

    @Test
    void loginIdUpdateTrimsThenUsesSameLoginIdPolicy() {
        assertThat(validator.validate(
                new LoginIdUpdateRequest("  new-login-id  ", "current-password")))
                .isEmpty();

        assertThat(validator.validate(
                new LoginIdUpdateRequest("new login", "current-password")))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString().equals("newLoginId"));
    }
}
