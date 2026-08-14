package app.mata.gradup.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.session.store-type", () -> "jdbc");
    registry.add("spring.session.jdbc.initialize-schema", () -> "always");
  }
}
