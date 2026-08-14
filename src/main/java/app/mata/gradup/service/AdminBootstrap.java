package app.mata.gradup.service;

import app.mata.gradup.model.Role;
import app.mata.gradup.repository.AdminRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JAdmin;
import app.mata.gradup.repository.model.JUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {

  private final UserRepository userRepository;
  private final AdminRepository adminRepository;
  private final PasswordEncoder passwordEncoder;
  private final String adminEmail;
  private final String adminPassword;

  public AdminBootstrap(
      UserRepository userRepository,
      AdminRepository adminRepository,
      PasswordEncoder passwordEncoder,
      @Value("${admin.email:}") String adminEmail,
      @Value("${admin.password:}") String adminPassword) {
    this.userRepository = userRepository;
    this.adminRepository = adminRepository;
    this.passwordEncoder = passwordEncoder;
    this.adminEmail = adminEmail;
    this.adminPassword = adminPassword;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (adminEmail == null
        || adminEmail.isBlank()
        || adminPassword == null
        || adminPassword.isBlank()) {
      return;
    }
    if (userRepository.findByEmail(adminEmail).isPresent()) {
      return;
    }
    var savedUser =
        userRepository.saveAndFlush(
            JUser.builder()
                .lastName("Administrator")
                .firstName("System")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .isActive(true)
                .build());
    adminRepository.save(JAdmin.builder().user(savedUser).build());
  }
}
