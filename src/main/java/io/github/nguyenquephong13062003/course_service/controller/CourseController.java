package io.github.nguyenquephong13062003.course_service.controller;

import io.github.nguyenquephong13062003.course_service.dto.CourseRequest;
import io.github.nguyenquephong13062003.course_service.dto.CourseResponse;
import io.github.nguyenquephong13062003.course_service.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Quản lý khóa học")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @Operation(summary = "Tạo khóa học")
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        CourseResponse created = courseService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Danh sách khóa học (phân trang: page, size, sort)")
    public PagedModel<CourseResponse> findAll(Pageable pageable) {
        return new PagedModel<>(courseService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy khóa học theo id")
    public CourseResponse findById(@PathVariable Long id) {
        return courseService.findById(id);
    }

    @GetMapping("/{id}/successors")
    @Operation(summary = "Danh sách khóa học sau của một khóa học")
    public List<CourseResponse> findSuccessors(@PathVariable Long id) {
        return courseService.findSuccessors(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật khóa học (thay thế toàn bộ)")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa khóa học; các khóa học sau sẽ được gỡ tiền đề")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
