package com.github.mybatis.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 类反射工具
 * Created by lirui on 15/1/7.
 */
public class ReflectionUtil {
  private static final LoadingCache<String, Field> CACHE = buildCache();

  private static LoadingCache<String, Field> buildCache() {
    return CacheBuilder.newBuilder().maximumSize(1000).build(new CacheLoader<String, Field>() {
      @Override
      public Field load(String key) throws Exception {
        int pos = key.lastIndexOf('#');
        String className = key.substring(0, pos);
        String fieldName = key.substring(pos + 1);
        Class clz = Class.forName(className);
        Field field = getDeclaredField(clz, fieldName);
        makeAccessible(field);
        return field;
      }
    });
  }

  /**
   * 循环向上转型,获取对象的DeclaredField.
   */
  public static void makeAccessible(final Field field) {
    if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
      field.setAccessible(true);
    }
  }

  /**
   * 循环向上转型,获取类对象的DeclaredField.
   */
  public static Field getDeclaredField(Class<?> clz, final String fieldName) {
    for (Class<?> s = clz; s != Object.class; s = s.getSuperclass()) {
      try {
        return s.getDeclaredField(fieldName);
      } catch (NoSuchFieldException ignored) {
      }
    }
    return null;
  }

  /**
   * 获取类属性，无视private/protected限制，不经过getter方法
   *
   * @param object    类对象
   * @param fieldName 属性名
   * @param <T>       目标类型
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T> T getFieldValue(Object object, String fieldName) {
    Field field = CACHE.getUnchecked(object.getClass().getName() + '#' + fieldName);
    if (field == null) {
      throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + object + "]");
    }

    Object result = null;
    try {
      result = field.get(object);
    } catch (IllegalAccessException ignored) {
    }
    return (T) result;
  }
}
