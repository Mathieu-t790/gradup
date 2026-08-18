package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.TranscriptGenerateRequest;
import app.mata.gradup.endpoint.rest.model.TranscriptResponse;
import app.mata.gradup.service.TranscriptService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TranscriptController {

  private final TranscriptService transcriptService;

  @PostMapping("/students/{studentId}/transcripts")
  public TranscriptResponse generateStudentTranscript(
      @PathVariable UUID studentId, @RequestBody @Valid TranscriptGenerateRequest request) {
    return transcriptService.generateStudentTranscript(studentId, request);
  }

  @GetMapping("/students/{studentId}/transcripts")
  public List<TranscriptResponse> listStudentTranscripts(@PathVariable UUID studentId) {
    return transcriptService.listStudentTranscripts(studentId);
  }
}
