package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.TrackCreateRequest;
import app.mata.gradup.endpoint.rest.model.TrackResponse;
import app.mata.gradup.service.TrackService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TrackController {

  private final TrackService trackService;

  @GetMapping("/tracks")
  public List<TrackResponse> listTracks() {
    return trackService.listTracks();
  }

  @PostMapping("/tracks")
  @ResponseStatus(HttpStatus.CREATED)
  public TrackResponse createTrack(@RequestBody @Valid TrackCreateRequest request) {
    return trackService.createTrack(request);
  }
}
