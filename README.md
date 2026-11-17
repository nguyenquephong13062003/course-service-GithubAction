# course-service-GithubAction

CRUD quản lý khóa học (Spring Boot 4.1.2, Java 21, Gradle Groovy, H2, JPA).

## 1. Build và chạy bình thường (không Docker)

Yêu cầu: JDK 21.

```bash
# Chạy test
./gradlew test

# Chạy ứng dụng trực tiếp
./gradlew bootRun

# Hoặc build file jar rồi chạy
./gradlew bootJar
java -jar build/libs/course-service-0.0.1-SNAPSHOT.jar
```

> Trên Windows dùng `gradlew.bat` thay cho `./gradlew`.

## 2. Build và chạy bằng Docker

Yêu cầu: Docker (Docker Compose đã tích hợp sẵn trong Docker mới).

**Cách 1 – Docker Compose (khuyên dùng):**

```bash
docker compose up --build -d     # build image và chạy nền
docker compose logs -f           # xem log
docker compose down              # dừng và xóa container
```

**Cách 2 – Docker thuần:**

```bash
docker build -t course-service .
docker run -d --name course-service -p 8080:8080 course-service
docker stop course-service && docker rm course-service
```

Dockerfile build theo 2 giai đoạn (JDK build jar, JRE chạy jar) và bỏ qua test khi build image.

## 3. Sau khi chạy

| Đường dẫn | Mô tả |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |
| http://localhost:8080/actuator/health | Kiểm tra sức khỏe |
| http://localhost:8080/h2-console | H2 console (JDBC URL `jdbc:h2:mem:coursedb`, user `sa`, password để trống) |

Lưu ý: dùng H2 in-memory nên dữ liệu mất khi khởi động lại ứng dụng/container.

## 4. API

| Method | Path | Mô tả |
|---|---|---|
| POST | /api/courses | Tạo khóa học (201 + Location) |
| GET | /api/courses?page&size&sort | Danh sách phân trang |
| GET | /api/courses/{id} | Chi tiết (gồm `prerequisiteId`, `successorIds`) |
| GET | /api/courses/{id}/successors | Các khóa học sau |
| PUT | /api/courses/{id} | Cập nhật toàn bộ |
| DELETE | /api/courses/{id} | Xóa; các khóa học sau bị gỡ tiền đề |

Lỗi trả về dạng ProblemDetail: 400 (dữ liệu sai, tiền đề không tồn tại hoặc gây vòng lặp), 404 (không tìm thấy), 409 (trùng tên).
