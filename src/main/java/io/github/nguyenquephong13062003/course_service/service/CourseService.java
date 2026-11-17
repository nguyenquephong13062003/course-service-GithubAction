package io.github.nguyenquephong13062003.course_service.service;

import io.github.nguyenquephong13062003.course_service.dto.CourseRequest;
import io.github.nguyenquephong13062003.course_service.dto.CourseResponse;
import io.github.nguyenquephong13062003.course_service.entity.Course;
import io.github.nguyenquephong13062003.course_service.exception.CourseNotFoundException;
import io.github.nguyenquephong13062003.course_service.exception.DuplicateCourseTitleException;
import io.github.nguyenquephong13062003.course_service.exception.InvalidPrerequisiteException;
import io.github.nguyenquephong13062003.course_service.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public CourseResponse create(CourseRequest request) {
        if (courseRepository.existsByTitleIgnoreCase(request.title())) {
            throw new DuplicateCourseTitleException(request.title());
        }
        Course course = new Course();
        apply(course, request);
        return CourseResponse.from(courseRepository.save(course));
    }

    public Page<CourseResponse> findAll(Pageable pageable) {
        return courseRepository.findAll(pageable).map(CourseResponse::from);
    }

    public CourseResponse findById(Long id) {
        return CourseResponse.from(getCourse(id));
    }

    public List<CourseResponse> findSuccessors(Long id) {
        return getCourse(id).getSuccessors().stream().map(CourseResponse::from).toList();
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = getCourse(id);
        if (courseRepository.existsByTitleIgnoreCaseAndIdNot(request.title(), id)) {
            throw new DuplicateCourseTitleException(request.title());
        }
        apply(course, request);
        return CourseResponse.from(course);
    }

    /** Xóa khóa học; các khóa học sau của nó được gỡ tiền đề (prerequisite = null). */
    @Transactional
    public void delete(Long id) {
        Course course = getCourse(id);
        for (Course successor : new ArrayList<>(course.getSuccessors())) {
            successor.setPrerequisite(null);
        }
        course.setPrerequisite(null);
        courseRepository.delete(course);
    }

    private void apply(Course course, CourseRequest request) {
        Course prerequisite = resolvePrerequisite(course, request.prerequisiteId());
        course.setTitle(request.title());
        course.setSummary(request.summary());
        course.setMaxStudents(request.maxStudents());
        course.replaceDocumentUrls(request.documentUrls());
        course.setPrerequisite(prerequisite);
    }

    /** Tìm khóa học trước và chặn tự tham chiếu / vòng lặp (A -> B -> ... -> A). */
    private Course resolvePrerequisite(Course course, Long prerequisiteId) {
        if (prerequisiteId == null) {
            return null;
        }
        Course prerequisite = courseRepository.findById(prerequisiteId)
                .orElseThrow(() -> new InvalidPrerequisiteException(
                        "Khóa học trước không tồn tại, id = " + prerequisiteId));
        for (Course current = prerequisite; current != null; current = current.getPrerequisite()) {
            boolean sameCourse = current == course
                    || (course.getId() != null && course.getId().equals(current.getId()));
            if (sameCourse) {
                throw new InvalidPrerequisiteException(
                        "Khóa học trước không hợp lệ: gây ra vòng lặp hoặc tự tham chiếu chính nó");
            }
        }
        return prerequisite;
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
    }
}
