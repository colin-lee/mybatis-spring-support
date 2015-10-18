package com.github.mybatis.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 存储的元信息
 *
 * Created by lirui on 2014/10/23.
 */
public class PersistMeta {
  /**
   * 数据库表名字
   */
  private String tableName;
  /**
   * 列名字到Field的映射
   */
  private Map<String, Field> columns;

  /**
   * 执行某个函数获取表名后缀
   */
  private Method postfix;

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public Map<String, Field> getColumns() {
    return columns;
  }

  public void setColumns(Map<String, Field> columns) {
    this.columns = columns;
  }

  public Method getPostfix() {
    return postfix;
  }

  public void setPostfix(Method postfix) {
    this.postfix = postfix;
  }
}
