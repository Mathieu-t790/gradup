package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JTranscript;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranscriptRepository extends JpaRepository<JTranscript, UUID> {

  List<JTranscript> findByStudentId(UUID studentId);
}
