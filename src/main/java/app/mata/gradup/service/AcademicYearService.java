package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.AcademicYearCreateRequest;
import app.mata.gradup.endpoint.rest.model.AcademicYearResponse;
import app.mata.gradup.exception.BadRequestException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.mapper.AcademicYearMapper;
import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AcademicYearService {

  private final AcademicYearRepository academicYearRepository;
  private final AcademicYearMapper academicYearMapper;

  @Transactional(readOnly = true)
  public List<AcademicYearResponse> listAcademicYears() {
    return academicYearRepository.findAll().stream()
        .map(academicYearMapper::toDomain)
        .map(academicYearMapper::toRest)
        .toList();
  }

  @Transactional
  public AcademicYearResponse createAcademicYear(AcademicYearCreateRequest request) {
    var label = request.getLabel();
    if (label == null || label.isBlank()) {
      throw new BadRequestException("Academic year label must not be blank");
    }
    if (!request.getStartDate().isBefore(request.getEndDate())) {
      throw new BadRequestException("Academic year start date must be before end date");
    }
    if (academicYearRepository.existsByLabel(label)) {
      throw new ConflictException("An academic year with label " + label + " already exists");
    }
    var saved =
        academicYearRepository.save(
            JAcademicYear.builder()
                .label(label)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build());
    return academicYearMapper.toRest(academicYearMapper.toDomain(saved));
  }
}
