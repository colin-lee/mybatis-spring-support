package com.github.mybatis.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EntityUtilTest {

  @Test
  public void testNameConvert() throws Exception {
    assertEquals("title", EntityUtil.nameConvert("title", true));
    assertEquals("access_date", EntityUtil.nameConvert("accessDate", true));
    assertEquals("http_status", EntityUtil.nameConvert("HTTPStatus", true));
  }
}
