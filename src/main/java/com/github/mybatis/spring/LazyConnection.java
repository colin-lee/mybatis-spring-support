package com.github.mybatis.spring;


import com.alibaba.druid.pool.DruidDataSource;
import com.github.mybatis.interceptor.MasterSlaveInterceptor;
import com.github.trace.TraceContext;
import com.google.common.base.CharMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

public class LazyConnection implements InvocationHandler {
  private static final Logger log = LoggerFactory.getLogger(LazyConnection.class);
  private DynamicDataSource router;
  private String username;
  private String password;
  private Boolean readOnly = Boolean.FALSE;
  private Integer transactionIsolation;
  private Boolean autoCommit = true;
  private boolean closed = false;
  private Connection target;

  public LazyConnection(DynamicDataSource router, boolean autoCommit) {
    this.router = router;
    this.autoCommit = autoCommit;
  }

  public LazyConnection(DynamicDataSource router, String username, String password, boolean autoCommit) {
    this(router, autoCommit);
    this.username = username;
    this.password = password;
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // Invocation on ConnectionProxy interface coming in...
    String methodName = method.getName();
    if (methodName.equals("equals")) {
      // We must avoid fetching a target Connection for "equals".
      // Only consider equal when proxies are identical.
      return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
    } else if (methodName.equals("hashCode")) {
      // We must avoid fetching a target Connection for "hashCode",
      // and we must return the same hash code even when the target
      // Connection has been fetched: use hashCode of Connection proxy.
      return System.identityHashCode(proxy);
    }

    if (!hasTargetConnection()) {
      // No physical target Connection kept yet ->
      // resolve transaction demarcation methods without fetching
      // a physical JDBC Connection until absolutely necessary.
      if (methodName.equals("toString")) {
        return "LazyConnection: " + router.getConfigName() + ", readOnly=" + this.readOnly;
      } else if (methodName.equals("isReadOnly")) {
        return this.readOnly;
      } else if (methodName.equals("setReadOnly")) {
        this.readOnly = (Boolean) args[0];
        return null;
      } else if (methodName.equals("getTransactionIsolation")) {
        if (this.transactionIsolation != null) {
          return this.transactionIsolation;
        }
        return Connection.TRANSACTION_READ_COMMITTED;
        // Else fetch actual Connection and check there,
        // because we didn't have a default specified.
      } else if (methodName.equals("setTransactionIsolation")) {
        this.transactionIsolation = (Integer) args[0];
        return null;
      } else if (methodName.equals("getAutoCommit")) {
        if (this.autoCommit != null)
          return this.autoCommit;
        return true;
        // Else fetch actual Connection and check there,
        // because we didn't have a default specified.
      } else if (methodName.equals("setAutoCommit")) {
        this.autoCommit = (Boolean) args[0];
        return null;
      } else if (methodName.equals("commit")) {
        // Ignore: no statements created yet.
        return null;
      } else if (methodName.equals("rollback")) {
        // Ignore: no statements created yet.
        return null;
      } else if (methodName.equals("getWarnings")) {
        return null;
      } else if (methodName.equals("clearWarnings")) {
        return null;
      } else if (methodName.equals("isClosed")) {
        return (this.closed ? Boolean.TRUE : Boolean.FALSE);
      } else if (methodName.equals("close")) {
        // Ignore: no target connection yet.
        this.closed = true;
        return null;
      } else if (this.closed) {
        // Connection proxy closed, without ever having fetched a
        // physical JDBC Connection: throw corresponding SQLException.
        throw new SQLException("Illegal operation: connection is closed");
      } else {
        // predicate: slave is always available
        target = getTargetConnection(method);
      }
    }

    // Target Connection already fetched,
    // or target Connection necessary for current operation ->
    // invoke method on target connection.
    try {
      return method.invoke(target, args);
    } catch (InvocationTargetException ex) {
      throw ex.getTargetException();
    } catch (Exception e) {
      log.error("[{}] method={}", router.getConfigName(), methodName, e);
      throw new RuntimeException("cannot invoke " + methodName, e);
    }
  }

  /**
   * Return whether the proxy currently holds a target Connection.
   */
  private boolean hasTargetConnection() {
    return (this.target != null);
  }

  /**
   * Return the target Connection, fetching it and initializing it if necessary.
   */
  private Connection getTargetConnection(Method operation) throws SQLException {
    if (this.target == null) {
      boolean readOnly = MasterSlaveInterceptor.isReadOnly();
      // No target Connection held -> fetch one.
      if (log.isDebugEnabled()) {
        log.debug("Connecting to database for operation '" + operation.getName() + "'");
      }

      // Fetch physical Connection from DataSource.
      DruidDataSource ds = router.determineTargetDataSource(readOnly);
      TraceContext.get().setServerName(router.getConfigName()).setUrl(extractHost(ds.getUrl()));
      this.target = (this.username != null) ? ds.getConnection(this.username, this.password) : ds.getConnection();

      // If we still lack default connection properties, check them now.
      //checkDefaultConnectionProperties(this.target);

      // Apply kept transaction settings, if any.
      if (this.readOnly) {
        this.target.setReadOnly(true);
      }
      if (this.transactionIsolation != null) {
        this.target.setTransactionIsolation(this.transactionIsolation);
      }
      if (this.autoCommit != null && this.autoCommit != this.target.getAutoCommit()) {
        this.target.setAutoCommit(this.autoCommit);
      }
    } else {
      // Target Connection already held -> return it.
      if (log.isDebugEnabled()) {
        log.debug("Using existing database connection for operation '" + operation.getName() + "'");
      }
    }

    return this.target;
  }

  private String extractHost(String url) {
    int start = url.startsWith("jdbc:") ? 5 : 0;
    int stop = CharMatcher.anyOf("?;").indexIn(url, start + 1);
    if (stop == -1) {
      stop = url.length();
    }
    return url.substring(start, stop);
  }
}
