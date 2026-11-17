package io.github.nguyenquephong13062003.course_service.repository;

import io.github.nguyenquephong13062003.course_service.entity.Course;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CourseRepositoryTest {

    @Autowired
    CourseRepository repository;

    @Autowired
    EntityManager em;

    private Course newCourse(String title) {
        Course c = new Course();
        c.setTitle(title);
        c.setMaxStudents(10);
        return c;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void save_persistsAllFields_andKeepsUrlOrder() {
        Course c = newCourse("Java cơ bản");
        c.setSummary("Tóm tắt khóa học");
        c.replaceDocumentUrls(List.of("https://example.com/3", "https://example.com/1", "https://example.com/2"));
        Long id = repository.save(c).getId();
        flushAndClear();

        Course found = repository.findById(id).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("Java cơ bản");
        assertThat(found.getSummary()).isEqualTo("Tóm tắt khóa học");
        assertThat(found.getMaxStudents()).isEqualTo(10);
        assertThat(found.getPrerequisite()).isNull();
        assertThat(found.getDocumentUrls())
                .containsExactly("https://example.com/3", "https://example.com/1", "https://example.com/2");
    }

    @Test
    void bidirectionalRelation_isLoadedFromBothSides() {
        Course parent = repository.save(newCourse("Java cơ bản"));
        Course child1 = newCourse("Java nâng cao");
        child1.setPrerequisite(parent);
        Course child2 = newCourse("Spring Boot");
        child2.setPrerequisite(parent);
        repository.save(child1);
        repository.save(child2);
        flushAndClear();

        Course loadedParent = repository.findById(parent.getId()).orElseThrow();
        Course loadedChild = repository.findById(child1.getId()).orElseThrow();

        assertThat(loadedParent.getPrerequisite()).isNull();
        assertThat(loadedParent.getSuccessors()).extracting(Course::getTitle)
                .containsExactlyInAnyOrder("Java nâng cao", "Spring Boot");
        assertThat(loadedChild.getPrerequisite().getId()).isEqualTo(parent.getId());
    }

    @Test
    void duplicateTitle_violatesUniqueConstraint() {
        repository.save(newCourse("Java cơ bản"));
        repository.save(newCourse("Java cơ bản"));

        assertThatThrownBy(em::flush).isInstanceOfAny(DataIntegrityViolationException.class,
                jakarta.persistence.PersistenceException.class);
    }

    @Test
    void existsByTitleIgnoreCase_ignoresCase() {
        repository.save(newCourse("Java cơ bản"));
        flushAndClear();

        assertThat(repository.existsByTitleIgnoreCase("JAVA CƠ BẢN")).isTrue();
        assertThat(repository.existsByTitleIgnoreCase("Python")).isFalse();
    }

    @Test
    void existsByTitleIgnoreCaseAndIdNot_excludesSelf() {
        Long id = repository.save(newCourse("Java cơ bản")).getId();
        Long otherId = repository.save(newCourse("Python cơ bản")).getId();
        flushAndClear();

        assertThat(repository.existsByTitleIgnoreCaseAndIdNot("java cơ bản", id)).isFalse();
        assertThat(repository.existsByTitleIgnoreCaseAndIdNot("java cơ bản", otherId)).isTrue();
    }

    @Test
    void emptyDocumentUrls_andNullSummary_areAllowed() {
        Long id = repository.save(newCourse("Java cơ bản")).getId();
        flushAndClear();

        Course found = repository.findById(id).orElseThrow();

        assertThat(found.getSummary()).isNull();
        assertThat(found.getDocumentUrls()).isEmpty();
    }
}
