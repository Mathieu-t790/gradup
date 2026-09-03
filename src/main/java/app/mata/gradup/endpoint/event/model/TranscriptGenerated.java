package app.mata.gradup.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class TranscriptGenerated extends PojaEvent {
  @JsonProperty("transcriptId")
  private UUID transcriptId;

  @JsonProperty("pdfData")
  private String pdfData;

  public TranscriptGenerated() {}

  public TranscriptGenerated(UUID transcriptId) {
    this.transcriptId = transcriptId;
  }

  public TranscriptGenerated(UUID transcriptId, String pdfData) {
    this.transcriptId = transcriptId;
    this.pdfData = pdfData;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
