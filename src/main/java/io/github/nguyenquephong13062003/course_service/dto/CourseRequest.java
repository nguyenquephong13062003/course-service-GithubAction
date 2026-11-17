package io.github.nguyenquephong13062003.course_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Schema(description = "Dữ liệu tạo/cập nhật khóa học")
public record CourseRequest(

        @Schema(description = "Tên khóa học, duy nhất (không phân biệt hoa thường)", example = "Java cơ bản")
        @NotBlank(message = "Tên khóa học là bắt buộc")
        @Size(min = 5, max = 50, message = "Tên khóa học phải từ 5 đến 50 ký tự")
        String title,

        @Schema(description = "Tóm tắt khóa học (không bắt buộc)", example = "Làm quen cú pháp và OOP trong Java")
        @Size(min = 10, max = 200, message = "Tóm tắt phải từ 10 đến 200 ký tự")
        String summary,

        @Schema(description = "Id khóa học trước (không bắt buộc)")
        @Positive(message = "Id khóa học trước phải là số dương")
        Long prerequisiteId,

        @Schema(description = "Số lượng học viên tối đa", example = "30")
        @NotNull(message = "Số lượng học viên tối đa là bắt buộc")
        @Min(value = 1, message = "Số lượng học viên tối đa tối thiểu là 1")
        Integer maxStudents,

        @Schema(description = "Danh sách URL tài liệu liên quan (không bắt buộc, tối đa 20)")
        @Size(max = 20, message = "Tối đa 20 URL tài liệu")
        List<@NotBlank(message = "URL không được để trống")
             @Size(max = 500, message = "Mỗi URL tối đa 500 ký tự")
             @URL(regexp = "^https?://.*", message = "URL phải hợp lệ và bắt đầu bằng http:// hoặc https://")
                     String> documentUrls
) {
}
