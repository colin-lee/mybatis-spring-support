package com.github.mybatis.util;

import com.github.mybatis.annotation.NameMappingPolicy;
import com.github.mybatis.annotation.TableSplitPolicy;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * 根据一些JPA注解获取Entity的方法
 *
 * Created by lirui on 2014/10/23.
 */
public final class EntityUtil {
  private static final Logger log = LoggerFactory.getLogger(EntityUtil.class);
  private static final ConcurrentMap<Class<?>, PersistMeta> meta = Maps.newConcurrentMap();

  private EntityUtil() {
  }

  public static String getTableName(Class<?> clazz) {
    return getMeta(clazz).getTableName();
  }

  public static Map<String, Field> getFields(Class<?> clazz) {
    return getMeta(clazz).getColumns();
  }

  public static PersistMeta getMeta(Class<?> clz) {
    PersistMeta PersistMeta = meta.get(clz);
    if (PersistMeta == null) {
      meta.putIfAbsent(clz, scanJpaAnnotations(clz));
    }
    return meta.get(clz);
  }

  /**
   * 扫描类注解信息
   *
   * @param clazz
   * @return
   */
  private static PersistMeta scanJpaAnnotations(Class<?> clazz) {
    //扫描名字处理策略
    boolean mapCamelCaseToUnderscore = true;
    NameMappingPolicy mapping = clazz.getAnnotation(NameMappingPolicy.class);
    if (null != mapping) {
      mapCamelCaseToUnderscore = "mapCamelCaseToUnderscore".equals(mapping.value());
    }

    PersistMeta meta = new PersistMeta();
    //扫描@Table注解获得表名字
    Table table = clazz.getAnnotation(Table.class);
    String tableName;
    if (null != table) {
      tableName = table.name();
    } else {
      tableName = nameConvert(clazz.getSimpleName(), mapCamelCaseToUnderscore);
    }
    meta.setTableName(tableName);
    log.info("Entity: {}, TableName: {}", clazz.getName(), tableName);

    //扫描分表操作，如果没有找到则不做处理
    TableSplitPolicy policy = clazz.getAnnotation(TableSplitPolicy.class);
    if (null != policy) {
      for (Class<?> clz = clazz; clz != Object.class; clz = clz.getSuperclass()) {
        try {
          Method method = clz.getDeclaredMethod(policy.value());
          method.setAccessible(true);
          meta.setPostfix(method);
          break;
        } catch (NoSuchMethodException ignored) {
        }
      }
    }

    //扫描@Column获取所有的数据表列，扫描类继承层次
    Map<String, Field> columns = Maps.newHashMap();
    final int ignoreModifier = Modifier.FINAL | Modifier.STATIC | Modifier.VOLATILE | Modifier.TRANSIENT;
    for (Class<?> clz = clazz; clz != Object.class; clz = clz.getSuperclass()) {
      Field[] fields = clz.getDeclaredFields();
      for (Field field : fields) {
        //有@Transient注解，代表不保存到db中
        if (field.isAnnotationPresent(Transient.class))
          continue;
        if ((field.getModifiers() & ignoreModifier) > 0)
          continue;

        String columnName;
        Column column = field.getAnnotation(Column.class);
        String fieldName = field.getName();
        if (column != null) {
          columnName = column.name();
          if (Strings.isNullOrEmpty(columnName)) {
            columnName = nameConvert(fieldName, mapCamelCaseToUnderscore);
          }
        } else {
          columnName = nameConvert(fieldName, mapCamelCaseToUnderscore);
        }
        columns.put(columnName, field);
        log.info("{}, fieldName:{}, columnName:{}", clazz.getName(), fieldName, columnName);
      }
    }
    meta.setColumns(columns);
    return meta;
  }

  /**
   * 将当前的类名转换成数据库表名或者列名。<br/>
   * 例如：Blog -> blog， HTTPClient -> http_client, BlogAuthor -> blog_author
   *
   * @return 转换后的名字
   */
  static String nameConvert(String name, boolean mapCamelCaseToUnderscore) {
    if (!mapCamelCaseToUnderscore)
      return name;
    StringBuilder sbd = new StringBuilder();
    char prevChar = name.charAt(0);
    sbd.append(Character.toLowerCase(prevChar));
    for (int i = 1, len = name.length(); i < len; i++) {
      char c = name.charAt(i);
      if (Character.isUpperCase(c)) {
        int next = i + 1;
        char nextChar = (next < len) ? name.charAt(next) : '\0';
        if (Character.isLowerCase(prevChar) || Character.isLowerCase(nextChar)) {
          sbd.append('_');
        }
        sbd.append(Character.toLowerCase(c));
      } else {
        sbd.append(c);
      }
      prevChar = c;
    }
    return sbd.toString();
  }
}
