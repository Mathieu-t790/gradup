package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.AcademicYearCreateRequest;
import app.mata.gradup.endpoint.rest.model.AcademicYearResponse;
import app.mata.gradup.service.AcademicYearService;
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
public class AcademicYearController {

  private final AcademicYearService academicYearService;

  @GetMapping("/academic-years")
  public List<AcademicYearResponse> listAcademicYears() {
    return academicYearService.listAcademicYears();
  }

  @PostMapping("/academic-years")
  @ResponseStatus(HttpStatus.CREATED)
  public AcademicYearResponse createAcademicYear(
      @RequestBody @Valid AcademicYearCreateRequest request) {
    return academicYearService.createAcademicYear(request);
  }
}
