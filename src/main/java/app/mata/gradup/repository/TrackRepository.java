package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JTrack;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackRepository extends JpaRepository<JTrack, UUID> {}
