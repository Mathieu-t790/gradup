package app.mata.gradup.service.utils;

import org.springframework.ui.Model;

public final class StudentLabels {

  private StudentLabels() {}

  public static void addGrades(Model model) {
    model.addAttribute("appName", Wording.get("promotion.web.app.name"));
    model.addAttribute("pageTitle", Wording.get("student.web.page.title"));
    model.addAttribute("pageSubtitle", Wording.get("student.web.page.subtitle"));
    model.addAttribute("columnCourse", Wording.get("student.web.column.course"));
    model.addAttribute("columnExam", Wording.get("student.web.column.exam"));
    model.addAttribute("columnScore", Wording.get("student.web.column.score"));
    model.addAttribute("columnRecordedBy", Wording.get("student.web.column.recorded.by"));
    model.addAttribute("columnRecordedAt", Wording.get("student.web.column.recorded.at"));
    model.addAttribute("filterLabel", Wording.get("student.web.filter.label"));
    model.addAttribute("filterAll", Wording.get("student.web.filter.all"));
    model.addAttribute("emptyTitle", Wording.get("student.web.empty.title"));
    model.addAttribute("emptyText", Wording.get("student.web.empty.text"));
    PromotionLabels.addSidebar(model);
  }
}
