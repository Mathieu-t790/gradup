package app.mata.gradup.repository.model;

import app.mata.gradup.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JUser {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "user_id")
  private UUID id;

  @Generated(event = EventType.INSERT)
  @Column(name = "reference", length = 10, insertable = false, updatable = false)
  private String reference;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "email", nullable = false, length = 255, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "phone", length = 30)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private Role role;

  @Generated(event = EventType.INSERT)
  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;
}
