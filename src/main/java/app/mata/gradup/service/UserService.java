package app.mata.gradup.service;

import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.mail.Email;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JUser;
import app.mata.gradup.service.utils.EmailAssets;
import app.mata.gradup.service.utils.HtmlTemplater;
import app.mata.gradup.service.utils.Wording;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final Mailer mailer;
  private final HtmlTemplater htmlTemplater;

  public UserCreation createUserWithRole(
      String lastName, String firstName, String email, Role role) {
    if (userRepository.findByEmail(email).isPresent()) {
      throw new ConflictException("A user with email " + email + " already exists");
    }
    var initialPassword = randomInitialPassword();
    var user =
        JUser.builder()
            .lastName(lastName)
            .firstName(firstName)
            .email(email)
            .passwordHash(passwordEncoder.encode(initialPassword))
            .role(role)
            .isActive(true)
            .build();
    JUser savedUser;
    try {
      savedUser = userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("A user with email " + email + " already exists");
    }
    return new UserCreation(savedUser, initialPassword);
  }

  public void sendCredentials(JUser user, String initialPassword) {
    Context context = new Context();
    context.setVariable("logoDataUri", EmailAssets.LOGO_DATA_URI);
    context.setVariable("signatureDataUri", EmailAssets.SIGNATURE_DATA_URI);
    context.setVariable("firstName", user.getFirstName());
    context.setVariable("email", user.getEmail());
    context.setVariable("password", initialPassword);
    String subject = Wording.get("credentials.subject", user.getReference());
    String htmlBody = htmlTemplater.render("email/credentials", context);
    try {
      mailer.accept(
          new Email(
              new InternetAddress(user.getEmail()),
              List.of(),
              List.of(),
              subject,
              htmlBody,
              List.of()));
    } catch (Exception e) {
      throw new RuntimeException("Could not send credentials for user " + user.getEmail(), e);
    }
  }

  private static String randomInitialPassword() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public record UserCreation(JUser user, String initialPassword) {}
}
