package io.github.nguyenquephong13062003.course_service.controller;

import io.github.nguyenquephong13062003.course_service.dto.CourseResponse;
import io.github.nguyenquephong13062003.course_service.exception.CourseNotFoundException;
import io.github.nguyenquephong13062003.course_service.exception.DuplicateCourseTitleException;
import io.github.nguyenquephong13062003.course_service.exception.InvalidPrerequisiteException;
import io.github.nguyenquephong13062003.course_service.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    private static final String VALID_BODY = """
            {"title":"Java cơ bản","summary":"Làm quen với Java","maxStudents":30,
             "documentUrls":["https://example.com/doc"]}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CourseService courseService;

    private static CourseResponse response(long id, String title) {
        return new CourseResponse(id, title, "Làm quen với Java", null, List.of(), 30,
                List.of("https://example.com/doc"));
    }

    @Test
    void create_valid_returns201WithLocation() throws Exception {
        when(courseService.create(any())).thenReturn(response(1, "Java cơ bản"));

        mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/courses/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Java cơ bản"));
    }

    @Test
    void create_invalidBody_returns400WithFieldErrors() throws Exception {
        String body = """
                {"title":"abc","summary":"ngắn","maxStudents":0,"documentUrls":["not-a-url"]}
                """;

        mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.summary").exists())
                .andExpect(jsonPath("$.errors.maxStudents").exists())
                .andExpect(jsonPath("$.errors['documentUrls[0]']").exists());
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.maxStudents").exists());
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_duplicateTitle_returns409() throws Exception {
        when(courseService.create(any())).thenThrow(new DuplicateCourseTitleException("Java cơ bản"));

        mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void create_invalidPrerequisite_returns400() throws Exception {
        when(courseService.create(any())).thenThrow(new InvalidPrerequisiteException("không tồn tại"));

        mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("không tồn tại"));
    }

    @Test
    void findAll_returnsPagedContent() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10);
        when(courseService.findAll(any()))
                .thenReturn(new PageImpl<>(List.of(response(1, "Java cơ bản")), pageable, 1));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java cơ bản"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void findById_found_returns200() throws Exception {
        when(courseService.findById(1L)).thenReturn(response(1, "Java cơ bản"));

        mockMvc.perform(get("/api/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java cơ bản"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(courseService.findById(9L)).thenThrow(new CourseNotFoundException(9L));

        mockMvc.perform(get("/api/courses/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void findById_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/courses/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void findSuccessors_returnsList() throws Exception {
        when(courseService.findSuccessors(1L)).thenReturn(List.of(response(2, "Java nâng cao")));

        mockMvc.perform(get("/api/courses/1/successors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void update_valid_returns200() throws Exception {
        when(courseService.update(eq(1L), any())).thenReturn(response(1, "Java cơ bản"));

        mockMvc.perform(put("/api/courses/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_invalidBody_returns400() throws Exception {
        mockMvc.perform(put("/api/courses/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(courseService.update(eq(9L), any())).thenThrow(new CourseNotFoundException(9L));

        mockMvc.perform(put("/api/courses/9").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/courses/1")).andExpect(status().isNoContent());

        verify(courseService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new CourseNotFoundException(9L)).when(courseService).delete(9L);

        mockMvc.perform(delete("/api/courses/9")).andExpect(status().isNotFound());
    }
}
