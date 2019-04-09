package com.github.hebelala.tasking.utils;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author hebelala
 */
public final class CloseableUtils {

  public static void closeQuietly(Closeable closeable) {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    } catch (IOException e) {
    }
  }

  /**
   * Invoke <code>close</code> method by reflection of <code>getMethod()</code>.
   */
  public static void closeQuietly(Object object) {
    if (object == null) {
      return;
    }
    try {
      object.getClass().getMethod("close").invoke(object);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
    }
  }

}
