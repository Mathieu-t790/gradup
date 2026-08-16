package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.TrackCreateRequest;
import app.mata.gradup.endpoint.rest.model.TrackResponse;
import app.mata.gradup.exception.BadRequestException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.mapper.TrackMapper;
import app.mata.gradup.repository.TrackRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TrackService {

  private final TrackRepository trackRepository;
  private final TrackMapper trackMapper;

  @Transactional(readOnly = true)
  public List<TrackResponse> listTracks() {
    return trackRepository.findAll().stream()
        .map(trackMapper::toDomain)
        .map(trackMapper::toRest)
        .toList();
  }

  @Transactional
  public TrackResponse createTrack(TrackCreateRequest request) {
    if (request.getLabel() == null || request.getLabel().isBlank()) {
      throw new BadRequestException("Track label must not be blank");
    }
    if (request.getCode() == null) {
      throw new BadRequestException("Track code must not be null");
    }
    var track = trackMapper.toDomain(request);
    if (trackRepository.existsByCode(track.code())) {
      throw new ConflictException("A track with code " + track.code() + " already exists");
    }
    var saved = trackRepository.save(trackMapper.toEntity(track));
    return trackMapper.toRest(trackMapper.toDomain(saved));
  }
}
