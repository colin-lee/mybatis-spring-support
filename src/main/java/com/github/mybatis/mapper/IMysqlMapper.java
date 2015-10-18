package com.github.mybatis.mapper;

import org.apache.ibatis.annotations.Select;

/**
 * mysql特定的一些查询
 * Created by lirui on 2014/10/30.
 */
public interface IMysqlMapper {
  /**
   * mysql 4.1 之后的版本支持
   * <pre>
   * A SELECT statement may include a LIMIT clause to restrict the number of rows the server returns to the client.
   * In some cases, it is desirable to know how many rows the statement would have returned without the LIMIT,
   * but without running the statement again. To obtain this row count, include a SQL_CALC_FOUND_ROWS option in the
   * SELECT statement, and then invoke FOUND_ROWS() afterward:
   * mysql&gt; SELECT SQL_CALC_FOUND_ROWS * FROM tbl_name WHERE id &gt; 100 LIMIT 10;
   * mysql&gt; SELECT FOUND_ROWS();
   *
   * The second SELECT returns a number indicating how many rows the first SELECT would have returned had it been
   * written without the LIMIT clause.
   * </pre>
   * @return
   */
  @Select("SELECT FOUND_ROWS()")
  int foundRows();

  /**
   * mysql5.6版本后支持，在修改操作之后返回更新的行数
   *
   * @return
   */
  @Select("SELECT ROW_COUNT()")
  int affectedRows();

  /**
   * 自动返回最后一个INSERT或 UPDATE 查询中 AUTO_INCREMENT列设置的第一个表发生的值
   *
   * @return
   */
  @Select("SELECT LAST_INSERT_ID()")
  int lastInsertId();
}
