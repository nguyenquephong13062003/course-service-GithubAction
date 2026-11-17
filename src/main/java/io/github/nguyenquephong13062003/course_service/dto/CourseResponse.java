package io.github.nguyenquephong13062003.course_service.dto;

import io.github.nguyenquephong13062003.course_service.entity.Course;

import java.util.List;

public record CourseResponse(
        Long id,
        String title,
        String summary,
        Long prerequisiteId,
        List<Long> successorIds,
        int maxStudents,
        List<String> documentUrls
) {

    /** Phải gọi trong transaction vì có đọc các collection lazy. */
    public static CourseResponse from(Course course) {
        Course prerequisite = course.getPrerequisite();
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getSummary(),
                prerequisite == null ? null : prerequisite.getId(),
                course.getSuccessors().stream().map(Course::getId).toList(),
                course.getMaxStudents(),
                List.copyOf(course.getDocumentUrls()));
    }
}
