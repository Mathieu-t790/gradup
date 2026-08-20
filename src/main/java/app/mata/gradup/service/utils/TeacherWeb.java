package app.mata.gradup.service.utils;

public final class TeacherWeb {

  private TeacherWeb() {}

  public static Double parseScore(String value) {
    try {
      double score = Double.parseDouble(value.trim());
      return Double.isFinite(score) ? score : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static String errorMessage(String error) {
    if (error == null) {
      return null;
    }
    return switch (error) {
      case "score.invalid" -> Wording.get("teacher.web.error.score.invalid");
      default -> null;
    };
  }
}
