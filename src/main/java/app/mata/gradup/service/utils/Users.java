package app.mata.gradup.service.utils;

import app.mata.gradup.repository.model.JUser;

public final class Users {

  private Users() {}

  public static String fullName(JUser user) {
    return user.getFirstName() + " " + user.getLastName();
  }
}
