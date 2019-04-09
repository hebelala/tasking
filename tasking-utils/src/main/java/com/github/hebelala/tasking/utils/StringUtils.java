package com.github.hebelala.tasking.utils;

/**
 * @author hebelala
 */
public final class StringUtils {

  public static boolean isBlank(String str) {
    return str == null || str.trim().isEmpty();
  }

  public static boolean isNotBlank(String str) {
    return str != null && !str.trim().isEmpty();
  }

}
