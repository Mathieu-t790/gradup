package app.mata.gradup.service.utils;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public final class Wording {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

  private Wording() {}

  public static String get(String key, Object... args) {
    return MessageFormat.format(BUNDLE.getString(key), args);
  }
}
