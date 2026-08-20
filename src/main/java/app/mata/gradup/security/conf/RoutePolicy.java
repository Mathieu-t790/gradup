package app.mata.gradup.security.conf;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;
import static org.springframework.security.authorization.AuthorizationManagers.allOf;

import app.mata.gradup.security.authorization.GradeAuthorizer;
import app.mata.gradup.security.authorization.OfferingAuthorizer;
import app.mata.gradup.security.authorization.StudentAuthorizer;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class RoutePolicy {

  public static final String[] CSRF_IGNORED_PATHS =
      new String[] {
        "/students/**",
        "/cohorts/**",
        "/tracks",
        "/groups",
        "/academic-years",
        "/semesters/**",
        "/courses/**",
        "/course-offerings/**",
        "/teachers/**",
        "/exams/**",
        "/grades/**",
        "/disputes/**",
        "/health/**"
      };

  private final StudentAuthorizer studentAuthorizer;
  private final GradeAuthorizer gradeAuthorizer;
  private final OfferingAuthorizer offeringAuthorizer;

  public void configure(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          auth) {
    auth.requestMatchers(
            "/", "/login", "/ping", "/health/**", "/error", "/css/**", "/js/**", "/images/**")
        .permitAll()
        .requestMatchers(
            HttpMethod.GET,
            "/cohorts",
            "/cohorts/{cohortId}",
            "/tracks",
            "/groups",
            "/academic-years",
            "/semesters",
            "/courses",
            "/courses/{courseId}")
        .authenticated()
        .requestMatchers(
            HttpMethod.POST,
            "/cohorts",
            "/tracks",
            "/groups",
            "/academic-years",
            "/semesters",
            "/semesters/{semesterId}/finalize",
            "/students",
            "/students/{studentId}/group-history",
            "/students/{studentId}/track-history",
            "/students/{studentId}/transcripts",
            "/students/{studentId}/transcripts/{transcriptId}/send",
            "/teachers",
            "/courses",
            "/course-offerings",
            "/course-offerings/{offeringId}/teachers/{teacherId}",
            "/cohorts/{cohortId}/diplomas/generate",
            "/promotions/**")
        .hasRole("ADMIN")
        .requestMatchers(
            HttpMethod.GET,
            "/teachers",
            "/cohorts/{cohortId}/diplomas",
            "/cohorts/{cohortId}/diplomas/export",
            "/promotions",
            "/promotions/{cohortId}")
        .hasRole("ADMIN")
        .requestMatchers(
            HttpMethod.PATCH, "/students/{studentId}", "/courses/{courseId}", "/cohorts/{cohortId}")
        .hasRole("ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/course-offerings/{offeringId}/teachers/{teacherId}")
        .hasRole("ADMIN")
        .requestMatchers(
            HttpMethod.GET,
            "/students",
            "/teachers/{teacherId}/course-offerings",
            "/course-offerings",
            "/course-offerings/{offeringId}",
            "/disputes")
        .hasAnyRole("ADMIN", "TEACHER")
        .requestMatchers(
            HttpMethod.POST, "/course-offerings/{offeringId}/exams", "/exams/{examId}/grades")
        .access(offeringAuthorizer)
        .requestMatchers(HttpMethod.GET, "/exams/{examId}/grades")
        .access(offeringAuthorizer)
        .requestMatchers(HttpMethod.PATCH, "/exams/{examId}", "/disputes/{disputeId}")
        .access(offeringAuthorizer)
        .requestMatchers(HttpMethod.PUT, "/grades/{gradeId}")
        .access(offeringAuthorizer)
        .requestMatchers(HttpMethod.GET, "/course-offerings/{offeringId}/exams", "/exams/{examId}")
        .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
        .requestMatchers(
            HttpMethod.GET,
            "/students/{studentId}",
            "/students/{studentId}/group-history",
            "/students/{studentId}/track-history",
            "/students/{studentId}/grades")
        .access(studentAuthorizer)
        .requestMatchers(
            HttpMethod.GET,
            "/students/{studentId}/graduation-eligibility",
            "/students/{studentId}/transcripts",
            "/students/{studentId}/disputes")
        .access(studentAuthorizer)
        .requestMatchers(HttpMethod.GET, "/grades/{gradeId}/history")
        .access(gradeAuthorizer)
        .requestMatchers(HttpMethod.POST, "/grades/{gradeId}/disputes")
        .access(allOf(hasRole("STUDENT"), gradeAuthorizer))
        .requestMatchers(HttpMethod.GET, "/student/grades")
        .hasRole("STUDENT")
        .requestMatchers(HttpMethod.GET, "/teacher/courses", "/teacher/courses/{offeringId}")
        .hasRole("TEACHER")
        .requestMatchers(HttpMethod.POST, "/teacher/courses/**")
        .hasRole("TEACHER")
        .anyRequest()
        .authenticated();
  }
}
