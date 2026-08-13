package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<JUser, UUID> {

  Optional<JUser> findByEmail(String email);
}
