package io.github.nguyenquephong13062003.course_service.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CourseNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(CourseNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({DuplicateCourseTitleException.class, DataIntegrityViolationException.class})
    ResponseEntity<ProblemDetail> conflict(Exception ex) {
        String detail = ex instanceof DuplicateCourseTitleException
                ? ex.getMessage()
                : "Dữ liệu vi phạm ràng buộc toàn vẹn (ví dụ: trùng tên khóa học)";
        return problem(HttpStatus.CONFLICT, detail);
    }

    @ExceptionHandler(InvalidPrerequisiteException.class)
    ResponseEntity<ProblemDetail> invalidPrerequisite(InvalidPrerequisiteException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ProblemDetail> invalidSort(PropertyReferenceException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Tham số sort không hợp lệ: " + ex.getPropertyName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ");
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
