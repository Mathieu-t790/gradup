package app.mata.gradup.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.model.Role;
import app.mata.gradup.repository.AdminRepository;
import app.mata.gradup.service.AdminBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class AdminBootstrapIT extends SecuredFacadeIT {

  private static final String BOOTSTRAP_ADMIN_EMAIL = "bootstrap.admin@cu.te";
  private static final String BOOTSTRAP_ADMIN_PASSWORD = "my-cute-password";

  @Autowired private AdminBootstrap adminBootstrap;
  @Autowired private AdminRepository adminRepository;

  @DynamicPropertySource
  static void adminProperties(DynamicPropertyRegistry registry) {
    registry.add("admin.email", () -> BOOTSTRAP_ADMIN_EMAIL);
    registry.add("admin.password", () -> BOOTSTRAP_ADMIN_PASSWORD);
  }

  @Test
  void bootstrap_creates_first_admin_with_bcrypt_hash() {
    var user = userRepository.findByEmail(BOOTSTRAP_ADMIN_EMAIL).orElseThrow();
    assertEquals(Role.ADMIN, user.getRole());
    assertTrue(user.getIsActive());
    assertTrue(passwordEncoder.matches(BOOTSTRAP_ADMIN_PASSWORD, user.getPasswordHash()));
    assertNotEquals(BOOTSTRAP_ADMIN_PASSWORD, user.getPasswordHash());
    assertTrue(adminRepository.findById(user.getId()).isPresent());
  }

  @Test
  void bootstrap_is_idempotent_and_never_overwrites_existing_admin() {
    var before = userRepository.findByEmail(BOOTSTRAP_ADMIN_EMAIL).orElseThrow();
    adminBootstrap.run(new DefaultApplicationArguments(new String[] {}));
    var after = userRepository.findByEmail(BOOTSTRAP_ADMIN_EMAIL).orElseThrow();
    assertEquals(before.getId(), after.getId());
    assertEquals(before.getPasswordHash(), after.getPasswordHash());
    assertEquals(
        1,
        userRepository.findAll().stream()
            .filter(user -> BOOTSTRAP_ADMIN_EMAIL.equals(user.getEmail()))
            .count());
  }
}