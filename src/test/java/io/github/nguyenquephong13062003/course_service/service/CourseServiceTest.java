package io.github.nguyenquephong13062003.course_service.service;

import io.github.nguyenquephong13062003.course_service.dto.CourseRequest;
import io.github.nguyenquephong13062003.course_service.dto.CourseResponse;
import io.github.nguyenquephong13062003.course_service.entity.Course;
import io.github.nguyenquephong13062003.course_service.exception.CourseNotFoundException;
import io.github.nguyenquephong13062003.course_service.exception.DuplicateCourseTitleException;
import io.github.nguyenquephong13062003.course_service.exception.InvalidPrerequisiteException;
import io.github.nguyenquephong13062003.course_service.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    CourseRepository repository;

    @InjectMocks
    CourseService service;

    private static Course course(Long id, String title) {
        Course c = new Course();
        c.setId(id);
        c.setTitle(title);
        c.setMaxStudents(20);
        return c;
    }

    private static CourseRequest request(String title, Long prerequisiteId) {
        return new CourseRequest(title, "Tóm tắt khóa học", prerequisiteId, 30, List.of("https://example.com/doc"));
    }

    // ---------- create ----------

    @Test
    void create_withoutPrerequisite_savesAndMapsResponse() {
        when(repository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CourseResponse response = service.create(request("Java cơ bản", null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Java cơ bản");
        assertThat(response.summary()).isEqualTo("Tóm tắt khóa học");
        assertThat(response.maxStudents()).isEqualTo(30);
        assertThat(response.prerequisiteId()).isNull();
        assertThat(response.successorIds()).isEmpty();
        assertThat(response.documentUrls()).containsExactly("https://example.com/doc");
    }

    @Test
    void create_withPrerequisite_linksBothSides() {
        Course prerequisite = course(1L, "Java cơ bản");
        when(repository.findById(1L)).thenReturn(Optional.of(prerequisite));
        when(repository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        CourseResponse response = service.create(request("Java nâng cao", 1L));

        assertThat(response.prerequisiteId()).isEqualTo(1L);
        assertThat(prerequisite.getSuccessors()).hasSize(1);
        assertThat(prerequisite.getSuccessors().get(0).getId()).isEqualTo(2L);
    }

    @Test
    void create_duplicateTitle_throwsConflictAndDoesNotSave() {
        when(repository.existsByTitleIgnoreCase("Java cơ bản")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("Java cơ bản", null)))
                .isInstanceOf(DuplicateCourseTitleException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_prerequisiteNotFound_throwsInvalidPrerequisite() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("Java nâng cao", 99L)))
                .isInstanceOf(InvalidPrerequisiteException.class)
                .hasMessageContaining("99");
        verify(repository, never()).save(any());
    }

    // ---------- read ----------

    @Test
    void findById_found_returnsResponse() {
        when(repository.findById(1L)).thenReturn(Optional.of(course(1L, "Java cơ bản")));

        assertThat(service.findById(1L).title()).isEqualTo("Java cơ bản");
    }

    @Test
    void findById_notFound_throws() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void findAll_mapsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(course(1L, "Java cơ bản")), pageable, 1));

        Page<CourseResponse> page = service.findAll(pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(CourseResponse::title).containsExactly("Java cơ bản");
    }

    @Test
    void findSuccessors_returnsOnlyDirectSuccessors() {
        Course parent = course(1L, "Java cơ bản");
        Course child = course(2L, "Java nâng cao");
        child.setPrerequisite(parent);
        when(repository.findById(1L)).thenReturn(Optional.of(parent));

        assertThat(service.findSuccessors(1L)).extracting(CourseResponse::id).containsExactly(2L);
    }

    // ---------- update ----------

    @Test
    void update_changesFieldsAndReplacesUrls() {
        Course existing = course(1L, "Tên cũ của khóa");
        existing.replaceDocumentUrls(List.of("https://old.example.com"));
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        CourseResponse response = service.update(1L, request("Tên mới của khóa", null));

        assertThat(response.title()).isEqualTo("Tên mới của khóa");
        assertThat(response.maxStudents()).isEqualTo(30);
        assertThat(response.documentUrls()).containsExactly("https://example.com/doc");
    }

    @Test
    void update_notFound_throws() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, request("Java cơ bản", null)))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void update_titleUsedByAnotherCourse_throwsConflict() {
        when(repository.findById(1L)).thenReturn(Optional.of(course(1L, "Tên cũ của khóa")));
        when(repository.existsByTitleIgnoreCaseAndIdNot("Java cơ bản", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("Java cơ bản", null)))
                .isInstanceOf(DuplicateCourseTitleException.class);
    }

    @Test
    void update_selfAsPrerequisite_isRejected() {
        Course c = course(1L, "Java cơ bản");
        when(repository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.update(1L, request("Java cơ bản", 1L)))
                .isInstanceOf(InvalidPrerequisiteException.class);
    }

    @Test
    void update_prerequisiteCreatingCycle_isRejected() {
        // a <- b <- c  (c có tiền đề b, b có tiền đề a). Đặt tiền đề của a = c sẽ tạo vòng.
        Course a = course(1L, "Khóa học A");
        Course b = course(2L, "Khóa học B");
        Course c = course(3L, "Khóa học C");
        b.setPrerequisite(a);
        c.setPrerequisite(b);
        when(repository.findById(1L)).thenReturn(Optional.of(a));
        when(repository.findById(3L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.update(1L, request("Khóa học A", 3L)))
                .isInstanceOf(InvalidPrerequisiteException.class);
        assertThat(a.getPrerequisite()).isNull();
    }

    @Test
    void update_changePrerequisite_movesBetweenSuccessorLists() {
        Course oldParent = course(1L, "Khóa cha cũ");
        Course newParent = course(2L, "Khóa cha mới");
        Course child = course(3L, "Khóa con");
        child.setPrerequisite(oldParent);
        when(repository.findById(3L)).thenReturn(Optional.of(child));
        when(repository.findById(2L)).thenReturn(Optional.of(newParent));

        CourseResponse response = service.update(3L, request("Khóa con", 2L));

        assertThat(response.prerequisiteId()).isEqualTo(2L);
        assertThat(oldParent.getSuccessors()).isEmpty();
        assertThat(newParent.getSuccessors()).containsExactly(child);
    }

    @Test
    void update_removePrerequisite_clearsBothSides() {
        Course parent = course(1L, "Khóa cha");
        Course child = course(2L, "Khóa con");
        child.setPrerequisite(parent);
        when(repository.findById(2L)).thenReturn(Optional.of(child));

        CourseResponse response = service.update(2L, request("Khóa con", null));

        assertThat(response.prerequisiteId()).isNull();
        assertThat(parent.getSuccessors()).isEmpty();
    }

    // ---------- delete ----------

    @Test
    void delete_detachesSuccessorsThenDeletes() {
        Course parent = course(1L, "Khóa cha");
        Course child1 = course(2L, "Khóa con 1");
        Course child2 = course(3L, "Khóa con 2");
        child1.setPrerequisite(parent);
        child2.setPrerequisite(parent);
        when(repository.findById(1L)).thenReturn(Optional.of(parent));

        service.delete(1L);

        assertThat(child1.getPrerequisite()).isNull();
        assertThat(child2.getPrerequisite()).isNull();
        assertThat(parent.getSuccessors()).isEmpty();
        verify(repository).delete(parent);
    }

    @Test
    void delete_removesItselfFromPrerequisiteSuccessors() {
        Course parent = course(1L, "Khóa cha");
        Course child = course(2L, "Khóa con");
        child.setPrerequisite(parent);
        when(repository.findById(2L)).thenReturn(Optional.of(child));

        service.delete(2L);

        assertThat(parent.getSuccessors()).isEmpty();
        verify(repository).delete(child);
    }

    @Test
    void delete_notFound_throwsAndDeletesNothing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(CourseNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
