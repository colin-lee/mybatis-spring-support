package com.github.mybatis.util;

/**
 * 工具类
 * Created by lirui on 2015-10-18 10:44.
 */
public class CloseableUtil {
  private CloseableUtil() {
  }

  public static void closeQuietly(AutoCloseable c) {
    if (c != null) {
      try {
        c.close();
      } catch (Exception ignored) {
      }
    }
  }
}
