package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.GroupCreateRequest;
import app.mata.gradup.endpoint.rest.model.GroupResponse;
import app.mata.gradup.exception.BadRequestException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.GroupMapper;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JTrack;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class GroupService {

  private final GroupRepository groupRepository;
  private final CohortRepository cohortRepository;
  private final TrackRepository trackRepository;
  private final GroupMapper groupMapper;

  @Transactional(readOnly = true)
  public List<GroupResponse> listGroups(UUID cohortId, UUID trackId) {
    return groupRepository.findAll().stream()
        .filter(group -> cohortId == null || group.getCohort().getId().equals(cohortId))
        .filter(
            group ->
                trackId == null
                    || (group.getTrack() != null && group.getTrack().getId().equals(trackId)))
        .map(groupMapper::toDomain)
        .map(groupMapper::toRest)
        .toList();
  }

  @Transactional
  public GroupResponse createGroup(GroupCreateRequest request) {
    var reference = request.getReference();
    if (reference == null || reference.isBlank()) {
      throw new BadRequestException("Group reference must not be blank");
    }
    var cohortId = request.getCohortId();
    if (cohortId == null) {
      throw new BadRequestException("Cohort must be specified");
    }
    var cohort =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(() -> new NotFoundException("Cohort not found"));
    var trackId = request.getTrackId();
    JTrack track = null;
    if (trackId != null) {
      track =
          trackRepository
              .findById(trackId)
              .orElseThrow(() -> new NotFoundException("Track not found"));
    }
    boolean duplicate =
        groupRepository.findByCohortId(cohortId).stream()
            .anyMatch(existing -> existing.getReference().equals(reference));
    if (duplicate) {
      throw new ConflictException(
          "A group with reference " + reference + " already exists in this cohort");
    }

    var saved =
        groupRepository.save(
            JGroup.builder().reference(reference).cohort(cohort).track(track).build());
    return groupMapper.toRest(groupMapper.toDomain(saved));
  }
}
