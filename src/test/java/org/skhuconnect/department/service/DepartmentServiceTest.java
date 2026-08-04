package org.skhuconnect.department.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skhuconnect.department.dto.response.DepartmentResponse;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentRepository);
    }

    @Test
    void getDepartmentsReturnsResponsesInRepositoryOrder() {
        Department software = department(1L, "SOFTWARE", "소프트웨어융합학부");
        Department humanities = department(2L, "HUMANITIES", "인문융합콘텐츠학부");
        when(departmentRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(software, humanities));

        List<DepartmentResponse> result = departmentService.getDepartments();

        assertThat(result).containsExactly(
                new DepartmentResponse(1L, "SOFTWARE", "소프트웨어융합학부"),
                new DepartmentResponse(2L, "HUMANITIES", "인문융합콘텐츠학부")
        );
        verify(departmentRepository).findAllByOrderByNameAsc();
    }

    @Test
    void getDepartmentsReturnsEmptyListWhenNoDepartmentExists() {
        when(departmentRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        List<DepartmentResponse> result = departmentService.getDepartments();

        assertThat(result).isEmpty();
        verify(departmentRepository).findAllByOrderByNameAsc();
    }

    private Department department(Long id, String code, String name) {
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(id);
        when(department.getCode()).thenReturn(code);
        when(department.getName()).thenReturn(name);
        return department;
    }
}
