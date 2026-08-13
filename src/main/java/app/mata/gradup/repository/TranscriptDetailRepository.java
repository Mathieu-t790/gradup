package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JTranscriptDetail;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranscriptDetailRepository extends JpaRepository<JTranscriptDetail, UUID> {

  List<JTranscriptDetail> findByTranscriptId(UUID transcriptId);
}
