package app.mata.gradup.service.utils;

import app.mata.gradup.endpoint.rest.model.CohortUpdateRequest;
import java.util.UUID;

public final class PromotionWeb {

  private PromotionWeb() {}

  public static Integer parseYear(String value) {
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static String errorMessage(String error) {
    if (error == null) {
      return null;
    }
    return switch (error) {
      case "not.finished" -> Wording.get("promotion.web.error.not.finished");
      case "label.required" -> Wording.get("promotion.web.error.label.required");
      case "year.invalid" -> Wording.get("promotion.web.error.year.invalid");
      default -> null;
    };
  }

  public static String redirectListError(String error) {
    return "redirect:/promotions?error=" + error;
  }

  public static String redirectDetail(UUID cohortId, String track, UUID groupId, String error) {
    StringBuilder query = new StringBuilder();
    if (track != null) {
      query.append("track=").append(track);
    }
    if (groupId != null) {
      query.append(query.isEmpty() ? "" : "&").append("group=").append(groupId);
    }
    if (error != null) {
      query.append(query.isEmpty() ? "" : "&").append("error=").append(error);
    }
    return "redirect:/promotions/" + cohortId + (query.isEmpty() ? "" : "?" + query);
  }

  public record PromotionForm(CohortUpdateRequest request, String error) {
    public static PromotionForm of(String label, String entryYear, String graduationYear) {
      CohortUpdateRequest request = new CohortUpdateRequest();
      String error = null;
      if (label != null) {
        if (label.isBlank()) {
          error = "label.required";
        } else {
          request.label(label.trim());
        }
      }
      if (entryYear != null && !entryYear.isBlank()) {
        Integer year = PromotionWeb.parseYear(entryYear);
        if (year == null) {
          error = "year.invalid";
        } else {
          request.entryYear(year);
        }
      }
      if (graduationYear != null && !graduationYear.isBlank()) {
        Integer year = PromotionWeb.parseYear(graduationYear);
        if (year == null) {
          error = "year.invalid";
        } else {
          request.expectedGraduationYear(year);
        }
      }
      return new PromotionForm(request, error);
    }
  }
}
