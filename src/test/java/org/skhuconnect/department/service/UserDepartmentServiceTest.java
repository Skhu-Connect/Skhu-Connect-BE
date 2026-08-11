package org.skhuconnect.department.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.exception.UserDepartmentException;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDepartmentServiceTest {
    private UserRepository userRepository;
    private DepartmentRepository departmentRepository;
    private UserDepartmentService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        service = new UserDepartmentService(userRepository, departmentRepository);
    }

    @Test
    void updateDepartmentChangesOnlyUserDepartment() {
        Department current = Department.create("SOFTWARE", "소프트웨어융합학부");
        Department changed = Department.create("MEDIA", "미디어콘텐츠융합학부");
        User user = User.create(
                "student@office.skhu.ac.kr", "student", "encoded-password", current);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(changed));

        service.updateDepartment(1L, 2L);

        assertThat(user.getDepartment()).isSameAs(changed);
        assertThat(user.getEmail()).isEqualTo("student@office.skhu.ac.kr");
        assertThat(user.getLoginId()).isEqualTo("student");
        verify(userRepository).findByIdForUpdate(1L);
        verify(departmentRepository).findById(2L);
    }

    @Test
    void updateDepartmentAllowsSameDepartment() {
        Department department = Department.create("SOFTWARE", "소프트웨어융합학부");
        User user = User.create(
                "student@office.skhu.ac.kr", "student", "encoded-password", department);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));

        service.updateDepartment(1L, 2L);

        assertThat(user.getDepartment()).isSameAs(department);
    }

    @Test
    void updateDepartmentRejectsUnknownDepartment() {
        Department current = Department.create("SOFTWARE", "소프트웨어융합학부");
        User user = User.create(
                "student@office.skhu.ac.kr", "student", "encoded-password", current);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateDepartment(1L, 999L))
                .isInstanceOf(UserDepartmentException.class)
                .extracting("reason")
                .isEqualTo(UserDepartmentException.Reason.DEPARTMENT_NOT_FOUND);
        assertThat(user.getDepartment()).isSameAs(current);
    }
}
