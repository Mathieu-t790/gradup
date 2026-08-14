package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.CohortCreateRequest;
import app.mata.gradup.endpoint.rest.model.CohortResponse;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.CohortMapper;
import app.mata.gradup.repository.CohortRepository;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional
public class CohortService {

  private final CohortRepository cohortRepository;
  private final CohortMapper cohortMapper;

  @Transactional(readOnly = true)
  public List<CohortResponse> listCohorts() {
    return cohortRepository.findAll().stream()
        .map(cohortMapper::toDomain)
        .map(cohortMapper::toRest)
        .toList();
  }

  public CohortResponse createCohort(CohortCreateRequest request) {
    var cohort = cohortMapper.toDomain(request);
    var jCohort = cohortMapper.toEntity(cohort);
    var saved = cohortRepository.save(jCohort);
    return cohortMapper.toRest(cohortMapper.toDomain(saved));
  }

  @Transactional(readOnly = true)
  public CohortResponse getCohort(UUID cohortId) {
    var jCohort =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(() -> new NotFoundException("Cohort not found with id: " + cohortId));
    return cohortMapper.toRest(cohortMapper.toDomain(jCohort));
  }
}
