package io.github.nguyenquephong13062003.course_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI courseServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Course Service API")
                .version("v1")
                .description("CRUD quản lý khóa học và quan hệ khóa học trước/sau"));
    }
}
