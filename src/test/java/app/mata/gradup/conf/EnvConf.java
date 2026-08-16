package app.mata.gradup.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.session.store-type", () -> "jdbc");
    registry.add("spring.session.jdbc.initialize-schema", () -> "always");
    registry.add("spring.data.web.pageable.default-page-size", () -> "50");
    registry.add("spring.data.web.pageable.max-page-size", () -> "200");
  }
}
