package app.mata.gradup.service.utils;

import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.model.JStudent;
import java.util.UUID;

public final class Students {

  private Students() {}

  public static JStudent requireStudent(StudentRepository studentRepository, UUID studentId) {
    return studentRepository
        .findById(studentId)
        .orElseThrow(() -> new NotFoundException("Student not found"));
  }
}
