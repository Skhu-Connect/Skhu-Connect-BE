package org.skhuconnect.department.service;

import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.exception.UserDepartmentException;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDepartmentService {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public UserDepartmentService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public void updateDepartment(Long userId, Long departmentId) {
        User user = userRepository.findByIdForUpdate(userId)
                .filter(foundUser -> !foundUser.isDeleted())
                .orElseThrow(() -> new UserDepartmentException(
                        UserDepartmentException.Reason.USER_NOT_FOUND));
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new UserDepartmentException(
                        UserDepartmentException.Reason.DEPARTMENT_NOT_FOUND));
        user.changeDepartment(department);
    }
}
