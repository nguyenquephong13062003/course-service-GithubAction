package io.github.nguyenquephong13062003.course_service.exception;

public class DuplicateCourseTitleException extends RuntimeException {
    public DuplicateCourseTitleException(String title) {
        super("Tên khóa học đã tồn tại: " + title);
    }
}
