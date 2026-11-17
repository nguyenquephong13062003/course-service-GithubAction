package io.github.nguyenquephong13062003.course_service.exception;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(Long id) {
        super("Không tìm thấy khóa học với id = " + id);
    }
}
