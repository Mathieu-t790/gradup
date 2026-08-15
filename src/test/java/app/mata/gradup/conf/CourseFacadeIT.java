package app.mata.gradup.conf;

import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JTrack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

public abstract class CourseFacadeIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected TrackRepository trackRepository;
  @Autowired protected UserRepository userRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @AfterEach
  void tearDown() {
    cleanDatabase();
  }

  private void cleanDatabase() {
    courseRepository.deleteAll();
    trackRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected JTrack saveTrack(String code, String label) {
    return trackRepository.save(
        JTrack.builder().code(app.mata.gradup.model.TrackCode.valueOf(code)).label(label).build());
  }

  protected JCourse saveCourse(
      String reference, String title, int credits, int semesterNumber, JTrack track) {
    return courseRepository.save(
        JCourse.builder()
            .reference(reference)
            .title(title)
            .credits(credits)
            .semesterNumber(semesterNumber)
            .track(track)
            .build());
  }
}
