package com.github.mybatis.mapper;

import com.github.mybatis.entity.TestDynamic;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 测试mapper
 * Created by lirui on 2014/10/31.
 */
public interface TestDynamicMapper extends ICrudPaginationMapper<TestDynamic> {
  @Select("select * from test_dynamic where name=#{name}")
  TestDynamic findByName(@Param("name") String name);
}
