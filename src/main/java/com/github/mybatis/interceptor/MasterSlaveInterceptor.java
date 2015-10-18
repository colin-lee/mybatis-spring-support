package com.github.mybatis.interceptor;

import com.github.mybatis.util.ReflectionUtil;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

import java.sql.Connection;
import java.util.Properties;

/**
 * 读写分离插件
 *
 * Created by lirui on 15/1/7.
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class})})
public class MasterSlaveInterceptor implements Interceptor {
  private static final ThreadLocal<Boolean> READ_ONLY_LOCAL = new ThreadLocal<Boolean>();

  public static boolean isReadOnly() {
    return Boolean.TRUE.equals(READ_ONLY_LOCAL.get());
  }

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
    MappedStatement mappedStatement;
    if (statementHandler instanceof RoutingStatementHandler) {
      StatementHandler delegate = ReflectionUtil.getFieldValue(statementHandler, "delegate");
      mappedStatement = ReflectionUtil.getFieldValue(delegate, "mappedStatement");
    } else {
      mappedStatement = ReflectionUtil.getFieldValue(statementHandler, "mappedStatement");
    }
    // 读写分离设置使用哪个数据源
    Boolean readOnly = Boolean.FALSE;
    if (mappedStatement.getSqlCommandType() == SqlCommandType.SELECT) {
      final String sql = statementHandler.getBoundSql().getSql();
      String s = sql.toLowerCase();
      readOnly = !(s.contains("last_insert_id()") || s.contains("row_count()"));
    }
    READ_ONLY_LOCAL.set(readOnly);

    return invocation.proceed();
  }

  @Override
  public Object plugin(Object target) {
    if (target instanceof StatementHandler) {
      return Plugin.wrap(target, this);
    } else {
      return target;
    }
  }

  @Override
  public void setProperties(Properties properties) {

  }

}
