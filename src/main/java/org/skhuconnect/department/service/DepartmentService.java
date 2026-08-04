package org.skhuconnect.department.service;

import org.skhuconnect.department.dto.response.DepartmentResponse;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments() {
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(DepartmentResponse::from)
                .toList();
    }
}
