package com.github.mybatis.mapper;

import com.github.mybatis.annotation.AutoResultMap;
import com.github.mybatis.pagination.Page;
import com.github.mybatis.provider.CrudProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

/**
 * Mapper基类
 */
public interface IPaginationMapper<T> {
  /**
   * 查找所有记录（分页）
   *
   * @param page 分页信息，查询后设置总条数
   * @return
   */
  @SelectProvider(type = CrudProvider.class, method = "findByPage")
  @AutoResultMap
  Page<T> pagination(@Param(CrudProvider.PAGE_KEY) Page page);

  /**
   * 按照过滤条件查找对应记录（分页）
   *
   * @param page        分页信息，查询后设置总条数
   * @param whereClause 查询条件
   * @return
   */
  @SelectProvider(type = CrudProvider.class, method = "findByPage")
  @AutoResultMap
  Page<T> pagination2(@Param(CrudProvider.PAGE_KEY) Page page, @Param(CrudProvider.WHERE_KEY) String whereClause);

  /**
   * 按照过滤条件查找对应记录（分页）
   *
   * @param page        分页信息，查询后设置总条数
   * @param whereClause 查询条件
   * @param orderClause 排序条件
   * @return
   */
  @SelectProvider(type = CrudProvider.class, method = "findByPage")
  @AutoResultMap
  Page<T> pagination3(@Param(CrudProvider.PAGE_KEY) Page page, @Param(CrudProvider.WHERE_KEY) String whereClause, @Param(CrudProvider.ORDER_KEY) String orderClause);

  /**
   * 按照过滤条件查找对应记录（分页）
   *
   * @param page        分页信息，查询后设置总条数
   * @param whereClause 查询条件
   * @param orderClause 排序条件
   * @return
   */
  @SelectProvider(type = CrudProvider.class, method = "findByPage")
  @AutoResultMap
  Page<T> pagination4(@Param(CrudProvider.PAGE_KEY) Page page, @Param(CrudProvider.WHERE_KEY) String whereClause, @Param(CrudProvider.ORDER_KEY) String orderClause, @Param(CrudProvider.GROUP_KEY) String groupClause);
}
