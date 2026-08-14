package app.mata.gradup.service.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;

public final class ClasspathImages {

  private ClasspathImages() {}

  public static String dataUri(String classpathPath) {
    try (InputStream in =
        ClasspathImages.class.getClassLoader().getResourceAsStream(classpathPath)) {
      if (in == null) {
        throw new IllegalStateException("Missing classpath resource: " + classpathPath);
      }
      return "data:"
          + mimeType(classpathPath)
          + ";base64,"
          + Base64.getEncoder().encodeToString(in.readAllBytes());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String mimeType(String classpathPath) {
    int dot = classpathPath.lastIndexOf('.');
    String extension = dot == -1 ? "" : classpathPath.substring(dot + 1).toLowerCase();
    return switch (extension) {
      case "png" -> "image/png";
      case "jpg", "jpeg" -> "image/jpeg";
      case "gif" -> "image/gif";
      case "svg" -> "image/svg+xml";
      default -> "application/octet-stream";
    };
  }
}
