package org.skhuconnect.auth.validation;

import java.util.regex.Pattern;

public final class AuthValidationPolicy {

    public static final String LOGIN_ID_PATTERN = "^[A-Za-z0-9._-]{5,20}$";
    public static final String PASSWORD_PATTERN =
            "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*?_]{5,20}$";

    public static final String LOGIN_ID_REQUIRED_MESSAGE = "아이디는 필수입니다.";
    public static final String LOGIN_ID_MESSAGE =
            "아이디는 5~20자의 영문 대소문자, 숫자, 특수문자(_, -, .)만 사용할 수 있습니다.";
    public static final String PASSWORD_REQUIRED_MESSAGE = "비밀번호는 필수입니다.";
    public static final String PASSWORD_MESSAGE =
            "비밀번호는 5~20자이며 영문과 숫자를 각각 1자 이상 포함하고 특수문자는 ! @ # $ % ^ & * ? _ 만 사용할 수 있습니다.";

    private static final Pattern LOGIN_ID = Pattern.compile(LOGIN_ID_PATTERN);
    private static final Pattern PASSWORD = Pattern.compile(PASSWORD_PATTERN);

    private AuthValidationPolicy() {
    }

    public static boolean isValidLoginId(String value) {
        return value != null && LOGIN_ID.matcher(value).matches();
    }

    public static boolean isValidPassword(String value) {
        return value != null && PASSWORD.matcher(value).matches();
    }
}
