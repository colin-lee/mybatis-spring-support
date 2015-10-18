package com.github.mybatis.mapper;

import com.github.mybatis.annotation.AutoResultMap;
import com.github.mybatis.annotation.FillEntityType;
import com.github.mybatis.provider.CrudProvider;
import org.apache.ibatis.annotations.*;

import java.io.Serializable;
import java.util.List;

/**
 * 提供基本的增删改查操作
 */
public interface ICrudMapper<T> {
  /**
   * 查找所有记录
   *
   * @return
   */
  @SelectProvider(type = CrudProvider.class, method = "findAll")
  @AutoResultMap
  List<T> findAll();

  /**
   * 统计所有记录行数
   *
   * @return
   */
  @SelectProvider(type = CrudProvider.class, method = "countAll")
  @FillEntityType
  int countAll();

  /**
   * 删除所有记录
   */
  @DeleteProvider(type = CrudProvider.class, method = "deleteAll")
  @FillEntityType
  int deleteAll();

  /**
   * 删除并创建一个同名的新表，可以回收innodb表空间，而且比deleteAll快，多用于测试case清理数据
   *
   * @return
   */
  @DeleteProvider(type = CrudProvider.class, method = "truncate")
  @AutoResultMap
  int truncate();

  /**
   * 根据主键查找记录
   *
   * @param id
   * @return
   */
  @SelectProvider(type = CrudProvider.class, method = "findById")
  @AutoResultMap
  T findById(Serializable id);

  /**
   * 插入记录
   *
   * @param t
   */
  @InsertProvider(type = CrudProvider.class, method = "insert")
  int insert(T t);

  /**
   * 插入记录并且取回自动生成的ID
   *
   * @param t
   */
  @InsertProvider(type = CrudProvider.class, method = "insert")
  @Options(useGeneratedKeys = true)
  int insertAndGetId(T t);

  /**
   * 更新记录
   *
   * @param t
   */
  @UpdateProvider(type = CrudProvider.class, method = "update")
  int update(T t);

  /**
   * 更新记录
   *
   * @param t
   */
  @UpdateProvider(type = CrudProvider.class, method = "save")
  int save(T t);

  /**
   * 删除记录
   *
   * @param t
   */
  @DeleteProvider(type = CrudProvider.class, method = "delete")
  int delete(T t);

  /**
   * 根据主键删除记录
   *
   * @param id
   */
  @DeleteProvider(type = CrudProvider.class, method = "deleteById")
  @AutoResultMap
  int deleteById(Serializable id);
}
