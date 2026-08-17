package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.repository.model.JExam;
import java.time.format.DateTimeFormatter;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExamMapper {

  DateTimeFormatter EXAM_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

  default ExamResponse toRest(JExam exam) {
    return new ExamResponse()
        .id(exam.getId())
        .offeringId(exam.getOffering().getId())
        .label(exam.getLabel())
        .examDate(exam.getExamDate())
        .examTime(
            exam.getExamTime() == null ? null : exam.getExamTime().format(EXAM_TIME_FORMATTER))
        .weightNumerator(exam.getWeightNumerator())
        .weightDenominator(exam.getWeightDenominator());
  }
}
