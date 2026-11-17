package io.github.nguyenquephong13062003.course_service.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CourseRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static CourseRequest valid() {
        return new CourseRequest("Java cơ bản", null, null, 10, null);
    }

    private static Set<String> invalidFields(CourseRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void minimalValidRequest_hasNoViolations() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    void fullValidRequest_hasNoViolations() {
        CourseRequest request = new CourseRequest("Java nâng cao", "Tóm tắt đủ mười ký tự", 1L, 1,
                List.of("https://example.com/a", "http://example.com/b.pdf"));
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void title_null_isInvalid() {
        assertThat(invalidFields(new CourseRequest(null, null, null, 10, null))).containsExactly("title");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abcd"})
    void title_blankOrTooShort_isInvalid(String title) {
        assertThat(invalidFields(new CourseRequest(title, null, null, 10, null))).contains("title");
    }

    @Test
    void title_boundaries() {
        assertThat(validator.validate(new CourseRequest("a".repeat(5), null, null, 10, null))).isEmpty();
        assertThat(validator.validate(new CourseRequest("a".repeat(50), null, null, 10, null))).isEmpty();
        assertThat(invalidFields(new CourseRequest("a".repeat(51), null, null, 10, null))).containsExactly("title");
    }

    @Test
    void summary_isOptional_butBoundedWhenPresent() {
        assertThat(validator.validate(new CourseRequest("Java cơ bản", null, null, 10, null))).isEmpty();
        assertThat(validator.validate(new CourseRequest("Java cơ bản", "a".repeat(10), null, 10, null))).isEmpty();
        assertThat(validator.validate(new CourseRequest("Java cơ bản", "a".repeat(200), null, 10, null))).isEmpty();
        assertThat(invalidFields(new CourseRequest("Java cơ bản", "a".repeat(9), null, 10, null)))
                .containsExactly("summary");
        assertThat(invalidFields(new CourseRequest("Java cơ bản", "a".repeat(201), null, 10, null)))
                .containsExactly("summary");
    }

    @Test
    void maxStudents_isRequiredAndAtLeastOne() {
        assertThat(invalidFields(new CourseRequest("Java cơ bản", null, null, null, null)))
                .containsExactly("maxStudents");
        assertThat(invalidFields(new CourseRequest("Java cơ bản", null, null, 0, null)))
                .containsExactly("maxStudents");
        assertThat(validator.validate(new CourseRequest("Java cơ bản", null, null, 1, null))).isEmpty();
    }

    @Test
    void prerequisiteId_mustBePositiveWhenPresent() {
        assertThat(invalidFields(new CourseRequest("Java cơ bản", null, 0L, 10, null)))
                .containsExactly("prerequisiteId");
        assertThat(invalidFields(new CourseRequest("Java cơ bản", null, -5L, 10, null)))
                .containsExactly("prerequisiteId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not a url", "ftp://example.com/file", "example.com", ""})
    void documentUrls_invalidItem_isRejected(String url) {
        assertThat(invalidFields(new CourseRequest("Java cơ bản", null, null, 10, List.of(url)))).isNotEmpty();
    }

    @Test
    void documentUrls_tooLongOrTooMany_isRejected() {
        String longUrl = "https://example.com/" + "a".repeat(500);
        assertThat(invalidFields(new CourseRequest("Java cơ bản", null, null, 10, List.of(longUrl)))).isNotEmpty();

        List<String> tooMany = java.util.stream.IntStream.rangeClosed(1, 21)
                .mapToObj(i -> "https://example.com/" + i).toList();
        assertThat(invalidFields(new CourseRequest("Java cơ bản", null, null, 10, tooMany)))
                .containsExactly("documentUrls");
    }
}
