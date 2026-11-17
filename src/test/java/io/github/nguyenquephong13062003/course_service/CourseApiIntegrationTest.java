package io.github.nguyenquephong13062003.course_service;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Kiểm thử end-to-end: controller -> service -> JPA -> H2. */
@SpringBootTest
@AutoConfigureMockMvc
class CourseApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("UPDATE courses SET prerequisite_id = NULL");
        jdbc.execute("DELETE FROM course_document_urls");
        jdbc.execute("DELETE FROM courses");
    }

    private static String body(String title, Long prerequisiteId, int maxStudents) {
        return """
                {"title":"%s","summary":"Tóm tắt của khóa học","prerequisiteId":%s,"maxStudents":%d,
                 "documentUrls":["https://example.com/doc"]}
                """.formatted(title, prerequisiteId, maxStudents);
    }

    private long createCourse(String title, Long prerequisiteId) throws Exception {
        String json = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON).content(body(title, prerequisiteId, 30)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void crudFlow() throws Exception {
        long id = createCourse("Java cơ bản", null);

        mockMvc.perform(get("/api/courses/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java cơ bản"))
                .andExpect(jsonPath("$.prerequisiteId").doesNotExist())
                .andExpect(jsonPath("$.documentUrls[0]").value("https://example.com/doc"));

        mockMvc.perform(put("/api/courses/" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Java căn bản", null, 50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java căn bản"))
                .andExpect(jsonPath("$.maxStudents").value(50));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(delete("/api/courses/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/courses/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateTitle_isRejectedCaseInsensitively() throws Exception {
        createCourse("Java cơ bản", null);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON).content(body("JAVA CƠ BẢN", null, 10)))
                .andExpect(status().isConflict());
    }

    @Test
    void update_keepingOwnTitle_isAllowed() throws Exception {
        long id = createCourse("Java cơ bản", null);

        mockMvc.perform(put("/api/courses/" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Java cơ bản", null, 99)))
                .andExpect(status().isOk());
    }

    @Test
    void relation_isTwoWay() throws Exception {
        long parent = createCourse("Java cơ bản", null);
        long child1 = createCourse("Java nâng cao", parent);
        long child2 = createCourse("Spring Boot", parent);

        mockMvc.perform(get("/api/courses/" + parent))
                .andExpect(jsonPath("$.successorIds", contains((int) child1, (int) child2)));
        mockMvc.perform(get("/api/courses/" + child1))
                .andExpect(jsonPath("$.prerequisiteId").value(parent));
        mockMvc.perform(get("/api/courses/" + parent + "/successors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void changingPrerequisite_updatesBothSides() throws Exception {
        long a = createCourse("Khóa học A", null);
        long b = createCourse("Khóa học B", null);
        long child = createCourse("Khóa học con", a);

        mockMvc.perform(put("/api/courses/" + child)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Khóa học con", b, 30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prerequisiteId").value(b));

        mockMvc.perform(get("/api/courses/" + a)).andExpect(jsonPath("$.successorIds", empty()));
        mockMvc.perform(get("/api/courses/" + b)).andExpect(jsonPath("$.successorIds", contains((int) child)));
    }

    @Test
    void deleteCourse_detachesItsSuccessors() throws Exception {
        long parent = createCourse("Java cơ bản", null);
        long child = createCourse("Java nâng cao", parent);

        mockMvc.perform(delete("/api/courses/" + parent)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/" + child))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prerequisiteId").doesNotExist());
    }

    @Test
    void deleteLeafCourse_removesItFromParentSuccessors() throws Exception {
        long parent = createCourse("Java cơ bản", null);
        long child = createCourse("Java nâng cao", parent);

        mockMvc.perform(delete("/api/courses/" + child)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/" + parent)).andExpect(jsonPath("$.successorIds", empty()));
    }

    @Test
    void selfPrerequisite_isRejected() throws Exception {
        long id = createCourse("Java cơ bản", null);

        mockMvc.perform(put("/api/courses/" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Java cơ bản", id, 30)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cyclicPrerequisite_isRejected() throws Exception {
        long a = createCourse("Khóa học A", null);
        long b = createCourse("Khóa học B", a);
        long c = createCourse("Khóa học C", b);

        mockMvc.perform(put("/api/courses/" + a)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Khóa học A", c, 30)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownPrerequisite_isRejected() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON).content(body("Java cơ bản", 12345L, 30)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void optionalFields_canBeOmitted() throws Exception {
        mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Java cơ bản\",\"maxStudents\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary").doesNotExist())
                .andExpect(jsonPath("$.documentUrls", empty()));
    }

    @Test
    void invalidSortProperty_returns400() throws Exception {
        mockMvc.perform(get("/api/courses?sort=doesNotExist,asc")).andExpect(status().isBadRequest());
    }

    @Test
    void openApiDocsAreExposed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Course Service API"))
                .andExpect(jsonPath("$.paths['/api/courses']").exists());
    }
}
