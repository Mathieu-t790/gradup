package app.mata.gradup.service.utils;

import org.springframework.ui.Model;

public final class PromotionLabels {

  private PromotionLabels() {}

  public static void addLanding(Model model) {
    model.addAttribute("appName", Wording.get("promotion.web.app.name"));
    model.addAttribute("landingTitle", Wording.get("landing.title"));
    model.addAttribute("landingSubtitle", Wording.get("landing.subtitle"));
    model.addAttribute("loginLabel", Wording.get("landing.login"));
    model.addAttribute("accessNote", Wording.get("landing.access.note"));
    model.addAttribute("footerLabel", Wording.get("landing.footer"));
  }

  public static void addList(Model model) {
    model.addAttribute("appName", Wording.get("promotion.web.app.name"));
    model.addAttribute("pageTitle", Wording.get("promotion.web.page.title"));
    model.addAttribute("pageSubtitle", Wording.get("promotion.web.page.subtitle"));
    model.addAttribute("columnLabel", Wording.get("promotion.web.table.label"));
    model.addAttribute("columnEntryYear", Wording.get("promotion.web.table.entry.year"));
    model.addAttribute("columnGraduationYear", Wording.get("promotion.web.table.graduation.year"));
    model.addAttribute("columnGraduates", Wording.get("promotion.web.table.graduates"));
    model.addAttribute("columnStatus", Wording.get("promotion.web.column.status"));
    model.addAttribute("columnActions", Wording.get("promotion.web.table.actions"));
    model.addAttribute("searchPlaceholder", Wording.get("promotion.web.search.placeholder"));
    model.addAttribute("createTitle", Wording.get("promotion.web.create.title"));
    model.addAttribute("createLabel", Wording.get("promotion.web.create.label"));
    model.addAttribute("createEntryYear", Wording.get("promotion.web.create.entry.year"));
    model.addAttribute("createGraduationYear", Wording.get("promotion.web.create.graduation.year"));
    model.addAttribute("createSubmit", Wording.get("promotion.web.create.submit"));
    model.addAttribute("statusFinished", Wording.get("promotion.web.status.finished"));
    model.addAttribute("statusInProgress", Wording.get("promotion.web.status.in.progress"));
    model.addAttribute("viewLabel", Wording.get("promotion.web.view"));
    model.addAttribute("editLabel", Wording.get("promotion.web.edit"));
    model.addAttribute("editTitle", Wording.get("promotion.web.edit.title"));
    model.addAttribute("editSubmit", Wording.get("promotion.web.edit.submit"));
    model.addAttribute("editCancel", Wording.get("promotion.web.edit.cancel"));
    model.addAttribute("logoutLabel", Wording.get("promotion.web.logout"));
    model.addAttribute("emptyTitle", Wording.get("promotion.web.empty.title"));
    model.addAttribute("emptyText", Wording.get("promotion.web.empty.text"));
    model.addAttribute("statPromotions", Wording.get("promotion.web.stat.promotions"));
    model.addAttribute("statGraduates", Wording.get("promotion.web.stat.graduates"));
    model.addAttribute("statTrackEl", Wording.get("promotion.web.stat.track", "EL"));
    model.addAttribute("statTrackTn", Wording.get("promotion.web.stat.track", "TN"));
  }

  public static void addDetail(Model model, int graduationAvailableFromYear) {
    model.addAttribute("appName", Wording.get("promotion.web.app.name"));
    model.addAttribute("pageTitle", Wording.get("promotion.web.detail.title"));
    model.addAttribute("backLabel", Wording.get("promotion.web.detail.back"));
    model.addAttribute(
        "notFinishedMessage",
        Wording.get("promotion.web.detail.not.finished", graduationAvailableFromYear));
    model.addAttribute("columnStatus", Wording.get("promotion.web.column.status"));
    model.addAttribute("statusFinished", Wording.get("promotion.web.status.finished"));
    model.addAttribute("statusInProgress", Wording.get("promotion.web.status.in.progress"));
    model.addAttribute("statusGraduated", Wording.get("promotion.web.status.graduated"));
    model.addAttribute("statusNotGraduated", Wording.get("promotion.web.status.not.graduated"));
    model.addAttribute("studentsTitle", Wording.get("promotion.web.detail.students"));
    model.addAttribute("graduatesTitle", Wording.get("promotion.web.detail.graduates"));
    model.addAttribute("graduatesEmpty", Wording.get("promotion.web.detail.graduates.empty"));
    model.addAttribute("statAverage", Wording.get("promotion.web.stat.average"));
    model.addAttribute("filterLabel", Wording.get("promotion.web.filter.label"));
    model.addAttribute("filterAll", Wording.get("promotion.web.filter.all"));
    model.addAttribute("generateLabel", Wording.get("promotion.web.generate"));
    model.addAttribute("downloadLabel", Wording.get("promotion.web.download"));
    model.addAttribute("columnRank", Wording.get("promotion.web.column.rank"));
    model.addAttribute("columnReference", Wording.get("promotion.web.column.reference"));
    model.addAttribute("columnLastName", Wording.get("promotion.web.column.last.name"));
    model.addAttribute("columnFirstName", Wording.get("promotion.web.column.first.name"));
    model.addAttribute("columnGroup", Wording.get("promotion.web.column.group"));
    model.addAttribute("columnTrack", Wording.get("promotion.web.column.track"));
    model.addAttribute("columnAverage", Wording.get("promotion.web.column.average"));
    model.addAttribute("logoutLabel", Wording.get("promotion.web.logout"));
  }
}
