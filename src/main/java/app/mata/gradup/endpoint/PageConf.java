package app.mata.gradup.endpoint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PageConf {

  @Bean
  public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer(
      @Value("${spring.data.web.pageable.default-page-size:50}") int defaultPageSize,
      @Value("${spring.data.web.pageable.max-page-size:200}") int maxPageSize) {
    return (PageableHandlerMethodArgumentResolver resolver) -> {
      resolver.setFallbackPageable(PageRequest.of(0, defaultPageSize));
      resolver.setMaxPageSize(maxPageSize);
    };
  }
}
