package com.github.mybatis.handler;

import com.github.mybatis.util.UnicodeFormatter;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 自定义字符串类型的转换，支持utf8mb4以上的字符，在存取的时候自动做uxxxx的转换
 * Created by lirui on 2014/9/28.
 */
public class StringTypeUtf8mb4Handler implements TypeHandler<String> {
  @Override
  public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
    ps.setString(i, UnicodeFormatter.unicodeUTF8mb4String(parameter));
  }

  @Override
  public String getResult(ResultSet rs, String columnName) throws SQLException {
    return UnicodeFormatter.decodeUnicodeString(rs.getString(columnName));
  }

  @Override
  public String getResult(ResultSet rs, int columnIndex) throws SQLException {
    return UnicodeFormatter.decodeUnicodeString(rs.getString(columnIndex));
  }

  @Override
  public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return UnicodeFormatter.decodeUnicodeString(cs.getString(columnIndex));
  }
}
