package com.github.mybatis.provider;

import com.github.mybatis.entity.IdEntity;
import com.github.mybatis.util.EntityUtil;
import com.github.mybatis.util.PersistMeta;
import org.apache.ibatis.jdbc.SQL;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Crud模板
 */
public class CrudProvider {
  public static final String CLASS_KEY = "clazz";
  public static final String PARA_KEY = "para";
  public static final String PAGE_KEY = "page";
  public static final String WHERE_KEY = "where";
  public static final String ORDER_KEY = "order";
  public static final String GROUP_KEY = "groupBy";

  /**
   * 查询所有记录
   *
   * @param clazz
   * @return
   */
  public String findAll(final Class<?> clazz) {
    return new SQL() {
      {
        SELECT("*");
        FROM(EntityUtil.getTableName(clazz));
      }
    }.toString();
  }

  /**
   * 统计所有记录行数
   *
   * @param clazz
   * @return
   */
  public String countAll(final Class<?> clazz) {
    return new SQL() {
      {
        SELECT("count(0)");
        FROM(EntityUtil.getTableName(clazz));
      }
    }.toString();
  }

  /**
   * 删除所有记录
   *
   * @param clazz
   * @return
   */
  public String deleteAll(final Class<?> clazz) {
    return new SQL() {
      {
        DELETE_FROM(EntityUtil.getTableName(clazz));
      }
    }.toString();
  }

  /**
   * 删除旧表，并新增一个同名的新表，比deleteAll要快，而且能回收innodb的表存储空间
   *
   * @param clazz
   * @return
   */
  public String truncate(final Class<?> clazz) {
    return "TRUNCATE TABLE " + EntityUtil.getTableName(clazz);
  }

  /**
   * 根据主键查找记录
   *
   * @param parameter
   * @return
   */
  public String findById(final Map<String, Object> parameter) {
    return new SQL() {
      {
        Class<?> clazz = (Class<?>) parameter.get(CLASS_KEY);
        SELECT("*");
        FROM(EntityUtil.getTableName(clazz));
        WHERE("id=#{" + PARA_KEY + '}');
      }
    }.toString();
  }

  /**
   * 查询所有记录(分页)
   *
   * @param parameter
   * @return
   */
  public String findByPage(final Map<String, Object> parameter) {
    return new SQL() {
      {
        Class<?> clazz = (Class<?>) parameter.get(CLASS_KEY);
        SELECT("*");
        FROM(EntityUtil.getTableName(clazz));
        if (parameter.containsKey(WHERE_KEY)) {
          WHERE((String) parameter.get(WHERE_KEY));
        }
        if (parameter.containsKey(ORDER_KEY)) {
          ORDER_BY((String) parameter.get(ORDER_KEY));
        }
        if (parameter.containsKey(GROUP_KEY)) {
          GROUP_BY((String) parameter.get(GROUP_KEY));
        }
      }
    }.toString();
  }

  /**
   * 按照id查找并删除某个对象
   *
   * @param obj
   * @return
   */
  public String delete(final Object obj) {
    final PersistMeta meta = EntityUtil.getMeta(obj.getClass());
    return new SQL() {
      {
        DELETE_FROM(getTableName(meta, obj));
        WHERE("id=#{id}");
      }
    }.toString();
  }

  /**
   * 根据主键删除记录
   *
   * @return
   */
  public String deleteById(final Map<String, Object> parameter) {
    return new SQL() {
      {
        Class<?> clazz = (Class<?>) parameter.get(CLASS_KEY);
        DELETE_FROM(EntityUtil.getTableName(clazz));
        WHERE("id=#{" + PARA_KEY + '}');
      }
    }.toString();
  }

  /**
   * 更新操作
   *
   * @param obj
   * @return String
   */
  public String update(Object obj) {
    Class<?> clazz = obj.getClass();
    PersistMeta meta = EntityUtil.getMeta(clazz);
    StringBuilder setting = new StringBuilder(32);
    int i = 0;
    for (Map.Entry<String, Field> kv : meta.getColumns().entrySet()) {
      if (isNull(kv.getValue(), obj)) {
        continue;
      }

      if (i++ != 0) {
        setting.append(',');
      }

      setting.append('`').append(kv.getKey()).append('`').append("=#{").append(kv.getValue().getName()).append('}');
    }

    return new SQL().UPDATE(getTableName(meta, obj)).SET(setting.toString()).WHERE("id=#{id}").toString();
  }

  /**
   * 新增操作
   *
   * @param obj
   * @return String
   */
  public String insert(Object obj) {
    PersistMeta meta = EntityUtil.getMeta(obj.getClass());
    StringBuilder names = new StringBuilder(), values = new StringBuilder();
    int i = 0;
    for (Map.Entry<String, Field> kv : meta.getColumns().entrySet()) {
      if (isNull(kv.getValue(), obj)) {
        continue;
      }

      if (i++ != 0) {
        names.append(',');
        values.append(',');
      }

      names.append('`').append(kv.getKey()).append('`');
      values.append("#{").append(kv.getValue().getName()).append('}');
    }

    return new SQL().INSERT_INTO(getTableName(meta, obj)).VALUES(names.toString(), values.toString()).toString();
  }

  private String getTableName(PersistMeta meta, Object obj) {
    if (meta.getPostfix() != null) {
      try {
        return meta.getTableName() + '_' + meta.getPostfix().invoke(obj);
      } catch (Exception ignored) {
      }
    }
    return meta.getTableName();
  }

  /**
   * 根据是否有id，决定调用insert或者update
   *
   * @param obj
   * @return
   */
  public String save(Object obj) {
    if (((IdEntity) obj).isNew())
      return insert(obj);
    else
      return update(obj);
  }

  /**
   * 列名判空处理
   *
   * @param field
   * @return boolean
   */
  private boolean isNull(Field field, Object obj) {
    try {
      if (!field.isAccessible()) {
        field.setAccessible(true);
      }
      return field.get(obj) == null;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return false;
  }
}
