package app.mata.gradup.security.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginPageFilterTest {

  private LoginPageFilter filter;

  @BeforeEach
  void setUp() {
    filter = new LoginPageFilter();
    filter.setLoginPageUrl("/login");
    filter.setFailureUrl("/login?error");
    filter.setLogoutSuccessUrl("/login");
    filter.setFormLoginEnabled(true);
  }

  private MockHttpServletRequest request(String uri, String queryString) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    if (queryString != null) {
      request.setQueryString(queryString);
    }
    return request;
  }

  @Test
  void empty_query_string_login_url_renders() throws Exception {
    var response = new MockHttpServletResponse();

    filter.doFilter(request("/login", ""), response, new MockFilterChain());

    assertEquals("text/html;charset=UTF-8", response.getContentType());
    assertTrue(response.getContentAsString().contains("Sign in"));
    assertTrue(response.getContentAsString().contains("form-signin"));
  }

  @Test
  void null_query_string_login_url_renders() throws Exception {
    var response = new MockHttpServletResponse();

    filter.doFilter(request("/login", null), response, new MockFilterChain());

    assertEquals("text/html;charset=UTF-8", response.getContentType());
    assertTrue(response.getContentAsString().contains("Sign in"));
  }

  @Test
  void error_query_renders_error_page() throws Exception {
    var response = new MockHttpServletResponse();

    filter.doFilter(request("/login", "error"), response, new MockFilterChain());

    assertTrue(response.getContentAsString().contains("Invalid credentials"));
    assertTrue(response.getContentAsString().contains("Sign in"));
  }

  @Test
  void unknown_query_passes_through() throws Exception {
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    filter.doFilter(request("/login", "x=1"), response, chain);

    assertFalse(response.getContentAsString().contains("Sign in"));
    assertNotNull(chain.getRequest());
  }
}
