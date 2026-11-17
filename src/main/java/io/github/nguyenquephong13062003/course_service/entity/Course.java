package io.github.nguyenquephong13062003.course_service.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Khóa học. Quan hệ tự tham chiếu hai chiều:
 * mỗi khóa học có tối đa 1 khóa học trước (prerequisite) và 0..n khóa học sau (successors).
 * Phía sở hữu khóa ngoại là {@code prerequisite}; luôn đổi quan hệ qua {@link #setPrerequisite(Course)}
 * để hai chiều luôn đồng bộ.
 */
@Entity
@Table(name = "courses",
        uniqueConstraints = @UniqueConstraint(name = "uk_courses_title", columnNames = "title"))
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(length = 200)
    private String summary;

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_id")
    private Course prerequisite;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "prerequisite")
    private List<Course> successors = new ArrayList<>();

    @Column(name = "max_students", nullable = false)
    private int maxStudents;

    @Setter(AccessLevel.NONE)
    @ElementCollection
    @CollectionTable(name = "course_document_urls", joinColumns = @JoinColumn(name = "course_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", nullable = false, length = 500)
    private List<String> documentUrls = new ArrayList<>();

    /** Đặt khóa học trước và cập nhật cả hai phía của quan hệ. */
    public void setPrerequisite(Course newPrerequisite) {
        if (this.prerequisite != null) {
            this.prerequisite.successors.remove(this);
        }
        this.prerequisite = newPrerequisite;
        if (newPrerequisite != null) {
            newPrerequisite.successors.add(this);
        }
    }

    public void replaceDocumentUrls(List<String> urls) {
        this.documentUrls.clear();
        if (urls != null) {
            this.documentUrls.addAll(urls);
        }
    }
}
