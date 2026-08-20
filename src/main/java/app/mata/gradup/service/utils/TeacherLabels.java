package app.mata.gradup.service.utils;

import org.springframework.ui.Model;

public final class TeacherLabels {

  private TeacherLabels() {}

  public static void addCourses(Model model) {
    model.addAttribute("appName", Wording.get("promotion.web.app.name"));
    model.addAttribute("pageTitle", Wording.get("teacher.web.page.title"));
    model.addAttribute("pageSubtitle", Wording.get("teacher.web.page.subtitle"));
    model.addAttribute("columnCourse", Wording.get("teacher.web.column.course"));
    model.addAttribute("columnGroup", Wording.get("teacher.web.column.group"));
    model.addAttribute("columnSemester", Wording.get("teacher.web.column.semester"));
    model.addAttribute("columnActions", Wording.get("teacher.web.column.actions"));
    model.addAttribute("entryLabel", Wording.get("teacher.web.entry"));
    model.addAttribute("filterLabel", Wording.get("teacher.web.filter.label"));
    model.addAttribute("filterAll", Wording.get("teacher.web.filter.all"));
    model.addAttribute("emptyTitle", Wording.get("teacher.web.empty.title"));
    model.addAttribute("emptyText", Wording.get("teacher.web.empty.text"));
    model.addAttribute("logoutLabel", Wording.get("promotion.web.logout"));
    PromotionLabels.addSidebar(model);
  }

  public static void addOffering(Model model) {
    model.addAttribute("appName", Wording.get("promotion.web.app.name"));
    model.addAttribute("backLabel", Wording.get("teacher.web.back"));
    model.addAttribute("examsTitle", Wording.get("teacher.web.exams.title"));
    model.addAttribute("columnReference", Wording.get("teacher.web.column.reference"));
    model.addAttribute("columnStudent", Wording.get("teacher.web.column.student"));
    model.addAttribute("columnScore", Wording.get("teacher.web.column.score"));
    model.addAttribute("columnAction", Wording.get("teacher.web.column.action"));
    model.addAttribute("saveLabel", Wording.get("teacher.web.save"));
    model.addAttribute("entryScoreLabel", Wording.get("teacher.web.entry.score"));
    model.addAttribute("searchPlaceholder", Wording.get("teacher.web.search.placeholder"));
    model.addAttribute("noStudents", Wording.get("teacher.web.no.students"));
    model.addAttribute("logoutLabel", Wording.get("promotion.web.logout"));
    PromotionLabels.addSidebar(model);
  }
}
