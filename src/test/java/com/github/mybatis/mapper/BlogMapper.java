package com.github.mybatis.mapper;

import com.github.mybatis.entity.Blog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface BlogMapper extends ICrudPaginationMapper<Blog> {
  @Delete("delete from blog")
  int myDelete();

  @Select("select count(0) from blog")
  int myCount();

  @Update("create table IF NOT EXISTS blog (\n" +
    "  id int PRIMARY KEY AUTO_INCREMENT NOT NULL,\n" +
    "  author VARCHAR(60) NOT NULL,\n" +
    "  content TEXT NOT NULL,\n" +
    "  access_date TIMESTAMP\n" +
    ")")
  int createTable();
}
