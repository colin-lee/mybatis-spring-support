package com.github.mybatis;

import com.github.mybatis.entity.Blog;
import com.github.mybatis.entity.TestDynamic;
import com.github.mybatis.entity.TestPage;
import com.github.mybatis.mapper.BlogMapper;
import com.github.mybatis.mapper.TestDynamicMapper;
import com.github.mybatis.mapper.TestPageMapper;
import com.github.mybatis.pagination.Page;
import com.github.mybatis.util.CloseableUtil;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author lirui
 * @since 2014年10月22日
 */
public class CrudTest {
  private static SqlSessionFactory sqlSessionFactory;

  @BeforeClass
  public static void beforeClass() throws Exception {
    InputStream inputStream = Resources.getResourceAsStream("mybatis/mybatis-config.xml");
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    initDb();
  }

  private static void initDb() throws IOException, SQLException {
    SqlSession session = null;
    try {
      session = sqlSessionFactory.openSession();
      File f = Resources.getResourceAsFile("database/h2.sql");
      List<String> lines = Files.readLines(f, Charset.defaultCharset());
      String sql = Joiner.on('\n').join(lines);
      PreparedStatement statement = session.getConnection().prepareStatement(sql);
      statement.executeUpdate();
    } finally {
      CloseableUtil.closeQuietly(session);
    }
  }

  @Test
  public void testAddUpdateDelete() throws Exception {
    Blog blog = new Blog();
    blog.setAuthor("colin");
    blog.setContent("test save");

    SqlSession session = sqlSessionFactory.openSession();
    try {
      BlogMapper mapper = session.getMapper(BlogMapper.class);

      assertTrue(0 == mapper.countAll());
      assertTrue(0 == mapper.myDelete());

      mapper.insert(blog); //不回填生成的id信息
      assertNull(blog.getId());

      mapper.insertAndGetId(blog);
      assertEquals(2, blog.getId().intValue());

      Blog n3 = new Blog();
      n3.setAuthor("lirui");
      n3.setContent("test insert2");
      n3.setAccessDate(new Date());
      mapper.insertAndGetId(n3);
      assertTrue(n3.getId() == 3);

      Blog first = mapper.findById(1);
      assertEquals("colin", first.getAuthor());

      first.setContent("test update");
      mapper.update(first);
      assertEquals("test update", mapper.findById(1).getContent());

      assertTrue(3 == mapper.countAll());
      assertEquals(3, mapper.myCount());

      mapper.deleteById(1);
      assertNull(mapper.findById(1));
      assertEquals(2, mapper.deleteAll());
    } finally {
      CloseableUtil.closeQuietly(session);
    }
  }

  @Test
  public void testPagination() throws Exception {
    SqlSession session = sqlSessionFactory.openSession();
    try {
      TestPageMapper mapper = session.getMapper(TestPageMapper.class);
      assertEquals(1, mapper.insert(new TestPage("github", "github-process")));
      assertEquals(1, mapper.insert(new TestPage("oschina", "oschina-process")));
      assertEquals(2, mapper.countAll());
      Page<TestPage> page1 = new Page<TestPage>(1, 1, false);
      mapper.pagination(page1);
      //System.out.println(page1);
      TestPage p1 = page1.get(0);
      assertEquals(1, p1.getId().intValue());
      assertEquals("github-process", p1.getName());
      Page<TestPage> page2 = new Page<TestPage>(2, 1);
      mapper.pagination(page2);
      //System.out.println(page2);
      TestPage p2 = page2.get(0);
      assertEquals(2, p2.getId().intValue());
      assertEquals("oschina-process", p2.getName());
    } finally {
      CloseableUtil.closeQuietly(session);
    }
  }

  @Test
  public void testDynamic() throws Exception {
    SqlSession session = sqlSessionFactory.openSession();
    try {
      TestDynamicMapper mapper = session.getMapper(TestDynamicMapper.class);
      mapper.findByName("colinli");
      assertEquals(1, mapper.insert(new TestDynamic("github", "github-process")));
      assertEquals(1, mapper.insert(new TestDynamic("github", "oschina-process")));
      assertEquals(2, mapper.countAll());
      TestDynamic byName = mapper.findByName("github-process");
      assertEquals("github-process", byName.getName());
    } finally {
      CloseableUtil.closeQuietly(session);
    }
  }
}
