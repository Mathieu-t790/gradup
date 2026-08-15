package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.CohortSummary;
import app.mata.gradup.endpoint.rest.model.DiplomaExportResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaPageResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaResponse;
import app.mata.gradup.endpoint.rest.model.StudentSummaryResponse;
import app.mata.gradup.endpoint.rest.model.TrackSummary;
import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.DiplomaRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTrack;
import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class DiplomaIT extends SecuredFacadeIT {

  private static final String BASE_URL = "/cohorts/%s/diplomas";
  private static final String GENERATE_URL = BASE_URL + "/generate";
  private static final String EXPORT_URL = BASE_URL + "/export";

  @MockBean private BucketComponent bucketComponent;

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @Autowired private DiplomaRepository diplomaRepository;

  @BeforeEach
  void setUp() throws Exception {
    reset(bucketComponent);
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
    when(bucketComponent.presign(any(), any()))
        .thenReturn(URI.create("http://localhost/diplomas.xlsx").toURL());
  }

  @Test
  void post_generate_freezes_rank_and_average_only_for_eligible_students() throws Exception {
    Fixture fixture = seed();

    DiplomaResponse[] diplomas = generate(fixture, TrackCode.EL);

    assertEquals(4, diplomas.length);
    assertDiploma(diplomas[0], 1, "STD21002", "Rabe", "Mialy", "15.00");
    assertDiploma(diplomas[1], 2, "STD21001", "Rakoto", "Hery", "14.00");
    assertDiploma(diplomas[2], 2, "STD21003", "Andria", "Tiana", "14.00");
    assertDiploma(diplomas[3], 4, "STD21004", "Rasolofoniaina", "Lova", "12.00");
    assertTrue(
        Arrays.stream(diplomas).noneMatch(d -> d.getStudent().getReference().equals("STD21005")),
        "student with a course below 10 must be absent");
    assertEquals(4, diplomaRepository.findAll().size());
  }

  @Test
  void post_generate_without_track_ranks_graduates_across_tracks_by_promotion() throws Exception {
    Fixture fixture = seed();

    DiplomaResponse[] diplomas = generate(fixture, null);

    assertEquals(5, diplomas.length);
    assertDiploma(diplomas[0], 1, "STD21002", "Rabe", "Mialy", "15.00");
    assertDiploma(diplomas[1], 2, "STD21001", "Rakoto", "Hery", "14.00");
    assertDiploma(diplomas[2], 2, "STD21003", "Andria", "Tiana", "14.00");
    assertDiploma(diplomas[3], 4, "STD21006", "Razafy", "Nirina", "13.00");
    assertDiploma(diplomas[4], 5, "STD21004", "Rasolofoniaina", "Lova", "12.00");
    assertTrue(
        Arrays.stream(diplomas).noneMatch(d -> d.getStudent().getReference().equals("STD21005")),
        "student with a course below 10 must be absent");
    assertEquals(5, diplomaRepository.findAll().size());
  }

  @Test
  void post_generate_is_idempotent() throws Exception {
    Fixture fixture = seed();

    generate(fixture, TrackCode.EL);
    generate(fixture, TrackCode.EL);

    assertEquals(4, diplomaRepository.findAll().size());
  }

  @Test
  void post_generate_removes_newly_ineligible_student_on_rerun() throws Exception {
    Fixture fixture = seed();
    generate(fixture, TrackCode.EL);
    assertEquals(4, diplomaRepository.findAll().size());

    seeder.changeScore("STD21003", "MOB1", new BigDecimal("7.00"));

    DiplomaResponse[] diplomas = generate(fixture, TrackCode.EL);

    assertEquals(3, diplomas.length);
    assertTrue(
        Arrays.stream(diplomas).noneMatch(d -> d.getStudent().getReference().equals("STD21003")),
        "newly ineligible student must be removed");
    assertEquals(3, diplomaRepository.findAll().size());
  }

  @Test
  void get_list_returns_paginated_and_track_filtered_diplomas() throws Exception {
    Fixture fixture = seed();
    generate(fixture, TrackCode.EL);
    generate(fixture, TrackCode.TN);

    ResponseEntity<DiplomaPageResponse> el =
        restTemplate.getForEntity(
            BASE_URL.formatted(fixture.cohortId) + "?track=EL&page=0&size=2",
            DiplomaPageResponse.class);
    assertEquals(HttpStatus.OK, el.getStatusCode());
    DiplomaPageResponse elPage = el.getBody();
    assertNotNull(elPage);
    assertEquals(2, elPage.getContent().size());
    assertEquals(4L, elPage.getTotalElements());
    assertEquals(0, elPage.getPage());
    assertEquals(2, elPage.getSize());
    assertEquals(2, elPage.getTotalPages());
    assertTrue(elPage.getFirst());
    assertEquals(false, elPage.getLast());

    ResponseEntity<DiplomaPageResponse> tn =
        restTemplate.getForEntity(
            BASE_URL.formatted(fixture.cohortId) + "?track=TN", DiplomaPageResponse.class);
    assertEquals(HttpStatus.OK, tn.getStatusCode());
    DiplomaPageResponse tnPage = tn.getBody();
    assertNotNull(tnPage);
    assertEquals(1, tnPage.getContent().size());
    assertEquals("STD21006", tnPage.getContent().getFirst().getStudent().getReference());
  }

  @Test
  void get_list_without_track_returns_all_graduates_of_the_cohort() throws Exception {
    Fixture fixture = seed();
    generate(fixture, TrackCode.EL);
    generate(fixture, TrackCode.TN);

    assertAllGraduates(fixture, "");
    assertAllGraduates(fixture, "?track=");
  }

  @Test
  void get_list_without_track_ranks_graduates_by_promotion() throws Exception {
    Fixture fixture = seed();
    generate(fixture, TrackCode.EL);
    generate(fixture, TrackCode.TN);

    String base = BASE_URL.formatted(fixture.cohortId);
    ResponseEntity<DiplomaPageResponse> all =
        restTemplate.getForEntity(base + "?page=0&size=50", DiplomaPageResponse.class);

    assertEquals(HttpStatus.OK, all.getStatusCode());
    DiplomaPageResponse allPage = all.getBody();
    assertNotNull(allPage);
    assertDiploma(allPage.getContent().get(0), 1, "STD21002", "Rabe", "Mialy", "15.00");
    assertDiploma(allPage.getContent().get(1), 2, "STD21001", "Rakoto", "Hery", "14.00");
    assertDiploma(allPage.getContent().get(2), 2, "STD21003", "Andria", "Tiana", "14.00");
    assertDiploma(allPage.getContent().get(3), 4, "STD21006", "Razafy", "Nirina", "13.00");
    assertDiploma(allPage.getContent().get(4), 5, "STD21004", "Rasolofoniaina", "Lova", "12.00");
  }

  private void assertAllGraduates(Fixture fixture, String trackQuery) throws Exception {
    String query = trackQuery == null ? "" : trackQuery;
    String base = BASE_URL.formatted(fixture.cohortId);
    String url = query.isEmpty() ? base + "?page=0&size=50" : base + query + "&page=0&size=50";
    ResponseEntity<DiplomaPageResponse> all =
        restTemplate.getForEntity(url, DiplomaPageResponse.class);

    assertEquals(HttpStatus.OK, all.getStatusCode());
    DiplomaPageResponse allPage = all.getBody();
    assertNotNull(allPage);
    assertEquals(5, allPage.getContent().size());
    assertEquals(5L, allPage.getTotalElements());
    assertTrue(
        allPage.getContent().stream()
            .map(d -> d.getStudent().getReference())
            .allMatch(
                ref ->
                    ref.equals("STD21001")
                        || ref.equals("STD21002")
                        || ref.equals("STD21003")
                        || ref.equals("STD21004")
                        || ref.equals("STD21006")));
  }

  @Test
  void get_export_returns_el_xlsx_uploaded_to_s3_matching_golden_file() throws Exception {
    Fixture fixture = seed();
    generate(fixture, TrackCode.EL);
    generate(fixture, TrackCode.TN);

    DiplomaExportResponse response = export(fixture, "?track=EL");

    assertNotNull(response);
    assertEquals("http://localhost/diplomas.xlsx", response.getDownloadUrl());
    assertTrue(response.getFileName().startsWith("diplomas/" + fixture.cohortId + "/"));
    assertTrue(
        response.getFileName().endsWith("dipl%C3%B4m%C3%A9s_Mpamakilay_EL.xlsx")
            || response.getFileName().endsWith("diplômés_Mpamakilay_EL.xlsx"));
    assertArrayEquals(seeder.goldenFile("xlsx/diplomas_el.xlsx"), uploadedContent());
  }

  @Test
  void get_export_returns_tn_xlsx_uploaded_to_s3_matching_golden_file() throws Exception {
    Fixture fixture = seed();
    generate(fixture, TrackCode.EL);
    generate(fixture, TrackCode.TN);

    DiplomaExportResponse response = export(fixture, "?track=TN");

    assertNotNull(response);
    assertEquals("http://localhost/diplomas.xlsx", response.getDownloadUrl());
    assertArrayEquals(seeder.goldenFile("xlsx/diplomas_tn.xlsx"), uploadedContent());
  }

  @Test
  void get_export_without_track_returns_all_graduates_matching_golden_file() throws Exception {
    Fixture fixture = seed();
    generate(fixture, TrackCode.EL);
    generate(fixture, TrackCode.TN);

    DiplomaExportResponse response = export(fixture, "");

    assertNotNull(response);
    assertEquals("http://localhost/diplomas.xlsx", response.getDownloadUrl());
    assertTrue(response.getFileName().startsWith("diplomas/" + fixture.cohortId + "/"));
    assertArrayEquals(seeder.goldenFile("xlsx/diplomas_all.xlsx"), uploadedContent());
  }

  @Test
  void get_export_with_empty_track_returns_all_graduates_matching_golden_file() throws Exception {
    Fixture fixture = seed();
    generate(fixture, TrackCode.EL);
    generate(fixture, TrackCode.TN);

    DiplomaExportResponse response = export(fixture, "?track=");

    assertNotNull(response);
    assertEquals("http://localhost/diplomas.xlsx", response.getDownloadUrl());
    assertArrayEquals(seeder.goldenFile("xlsx/diplomas_all.xlsx"), uploadedContent());
  }

  @Test
  void generate_unknown_cohort_returns_404() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            GENERATE_URL.formatted(UUID.randomUUID()) + "?track=EL", null, String.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  private DiplomaExportResponse export(Fixture fixture, String query) {
    ResponseEntity<DiplomaExportResponse> response =
        restTemplate.getForEntity(
            EXPORT_URL.formatted(fixture.cohortId) + query, DiplomaExportResponse.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    return response.getBody();
  }

  private byte[] uploadedContent() throws Exception {
    ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
    verify(bucketComponent).upload(fileCaptor.capture(), any());
    return Files.readAllBytes(fileCaptor.getValue().toPath());
  }

  private void assertDiploma(
      DiplomaResponse diploma,
      int rank,
      String reference,
      String lastName,
      String firstName,
      String average) {
    assertEquals(rank, diploma.getRank());
    StudentSummaryResponse student = diploma.getStudent();
    assertNotNull(student);
    assertEquals(reference, student.getReference());
    assertEquals(lastName, student.getLastName());
    assertEquals(firstName, student.getFirstName());
    assertNotNull(student.getCohortLabel());
    CohortSummary cohort = diploma.getCohort();
    assertNotNull(cohort);
    assertNotNull(cohort.getLabel());
    TrackSummary track = diploma.getTrack();
    assertNotNull(track);
    assertNotNull(track.getCode());
    assertNotNull(diploma.getOverallAverage());
    assertEquals(
        0, new BigDecimal(average).compareTo(BigDecimal.valueOf(diploma.getOverallAverage())));
    assertNotNull(diploma.getGraduationDate());
    assertNotNull(diploma.getListGeneratedAt());
  }

  private DiplomaResponse[] generate(Fixture fixture, TrackCode track) {
    String query = track == null ? "" : "?track=" + track;
    ResponseEntity<DiplomaResponse[]> response =
        restTemplate.postForEntity(
            GENERATE_URL.formatted(fixture.cohortId) + query, null, DiplomaResponse[].class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    return response.getBody();
  }

  private Fixture seed() {
    return seeder.inTransaction(
        () -> {
          JCohort cohort = seeder.cohort("Mpamakilay", 2021, 2024);
          JTrack el = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
          JTrack tn = seeder.track(TrackCode.TN, "Transformation Numerique");
          JGroup groupEl = seeder.group("G1", cohort, el);
          JGroup groupTn = seeder.group("G2", cohort, tn);
          JAcademicYear year =
              seeder.academicYear("2021-2024", LocalDate.of(2021, 9, 1), LocalDate.of(2024, 7, 31));
          JSemester s1 =
              seeder.semester(1, year, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
          JSemester s3 =
              seeder.semester(3, year, LocalDate.of(2022, 9, 1), LocalDate.of(2023, 1, 31));
          JSemester s5 =
              seeder.semester(5, year, LocalDate.of(2023, 9, 1), LocalDate.of(2024, 1, 31));

          JCourse prog1 = seeder.course("PROG1", 60, 1, null);
          JCourse prog3 = seeder.course("PROG3", 60, 3, null);
          JCourse mob1 = seeder.course("MOB1", 60, 5, el);
          JCourse tn4 = seeder.course("TN4", 60, 5, tn);

          JExam examProg1El = seeder.exam(seeder.offering(prog1, groupEl, s1));
          JExam examProg1Tn = seeder.exam(seeder.offering(prog1, groupTn, s1));
          JExam examProg3El = seeder.exam(seeder.offering(prog3, groupEl, s3));
          JExam examProg3Tn = seeder.exam(seeder.offering(prog3, groupTn, s3));
          JExam examMob1 = seeder.exam(seeder.offering(mob1, groupEl, s5));
          JExam examTn4 = seeder.exam(seeder.offering(tn4, groupTn, s5));

          JStudent std01 =
              seeder.student("STD21001", "Rakoto", "Hery", "hery@cu.te", cohort, el, groupEl);
          JStudent std02 =
              seeder.student("STD21002", "Rabe", "Mialy", "mialy@cu.te", cohort, el, groupEl);
          JStudent std03 =
              seeder.student("STD21003", "Andria", "Tiana", "tiana@cu.te", cohort, el, groupEl);
          JStudent std04 =
              seeder.student(
                  "STD21004", "Rasolofoniaina", "Lova", "lova@cu.te", cohort, el, groupEl);
          JStudent std05 =
              seeder.student(
                  "STD21005", "Randria", "Sariaka", "sariaka@cu.te", cohort, el, groupEl);
          JStudent std06 =
              seeder.student("STD21006", "Razafy", "Nirina", "nirina@cu.te", cohort, tn, groupTn);

          seeder.grade(std01, examProg1El, "12.00");
          seeder.grade(std01, examProg3El, "14.00");
          seeder.grade(std01, examMob1, "16.00");
          seeder.grade(std02, examProg1El, "15.00");
          seeder.grade(std02, examProg3El, "15.00");
          seeder.grade(std02, examMob1, "15.00");
          seeder.grade(std03, examProg1El, "14.00");
          seeder.grade(std03, examProg3El, "14.00");
          seeder.grade(std03, examMob1, "14.00");
          seeder.grade(std04, examProg1El, "12.00");
          seeder.grade(std04, examProg3El, "12.00");
          seeder.grade(std04, examMob1, "12.00");
          seeder.grade(std05, examProg1El, "14.00");
          seeder.grade(std05, examProg3El, "14.00");
          seeder.grade(std05, examMob1, "8.00");
          seeder.grade(std06, examProg1Tn, "13.00");
          seeder.grade(std06, examProg3Tn, "13.00");
          seeder.grade(std06, examTn4, "13.00");

          return new Fixture(cohort.getId());
        });
  }

  private record Fixture(UUID cohortId) {}
}
