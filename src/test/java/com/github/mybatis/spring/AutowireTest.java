package com.github.mybatis.spring;

import com.github.mybatis.entity.Blog;
import com.github.mybatis.mapper.BlogMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PostConstruct;
import java.util.Date;

import static org.junit.Assert.assertTrue;

/**
 * 测试autowire
 * Created by lirui on 15/1/7.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class AutowireTest {
  @Autowired
  private BlogMapper mapper;

  @PostConstruct
  void init() {
    mapper.createTable();
    mapper.truncate();
  }

  @Test
  public void testAutowire() throws Exception {
    Blog b = new Blog();
    b.setAuthor("colin");
    b.setContent("test content");
    b.setAccessDate(new Date());
    int num = mapper.insertAndGetId(b);
    assertTrue(1 == num);
  }
}
