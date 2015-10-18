package com.github.mybatis.spring;

import com.alibaba.druid.pool.DruidDataSource;
import com.github.autoconf.ConfigFactory;
import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IConfig;
import com.github.autoconf.api.IConfigFactory;
import com.github.mybatis.util.CloseableUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.reflect.Reflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 读取配置文件动态创建DataSource
 *
 * Created by lirui on 15/1/7.
 */
public class DynamicDataSource extends AbstractDataSource implements InitializingBean, DisposableBean {
  private static final Logger LOG = LoggerFactory.getLogger(DynamicDataSource.class);
  private IConfigFactory configFactory;
  private String configName;
  private boolean autoCommit = true;
  private DruidDataSource writer;
  private DruidDataSource reader;

  public String getConfigName() {
    return configName;
  }

  public void setConfigName(String configName) {
    this.configName = configName;
  }

  public IConfigFactory getConfigFactory() {
    return configFactory;
  }

  public void setConfigFactory(IConfigFactory configFactory) {
    this.configFactory = configFactory;
  }

  public boolean isAutoCommit() {
    return autoCommit;
  }

  public void setAutoCommit(boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  @Override
  public Connection getConnection() throws SQLException {
    LazyConnection connection = new LazyConnection(this, autoCommit);
    return Reflection.newProxy(Connection.class, connection);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    LazyConnection connection = new LazyConnection(this, username, password, autoCommit);
    return Reflection.newProxy(Connection.class, connection);
  }

  protected DruidDataSource determineTargetDataSource(boolean read) {
    return read ? reader : writer;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (configFactory == null) {
      configFactory = ConfigFactory.getInstance();
    }
    configFactory.getConfig(configName, new IChangeListener() {
      @Override
      public void changed(IConfig conf) {
        String masterUrl = conf.get("masterUrl");
        String username = conf.get("username");
        String password = conf.get("password");
        // 分开读和写的接口，这样互不影响，必要时可以停止读或者写
        DruidDataSource writer = buildDruidDataSource("master", masterUrl, username, password);
        String slaveUrl = conf.get("slaveUrl");
        if (Strings.isNullOrEmpty(slaveUrl)) {
          slaveUrl = masterUrl;
        }
        DruidDataSource reader = buildDruidDataSource("slave", slaveUrl, username, password);

        List<DruidDataSource> ds = Lists.newArrayList();
        if (DynamicDataSource.this.writer != null) {
          ds.add(DynamicDataSource.this.writer);
        }
        if (DynamicDataSource.this.reader != null) {
          ds.add(DynamicDataSource.this.reader);
        }
        DynamicDataSource.this.writer = writer;
        DynamicDataSource.this.reader = reader;
        for (DruidDataSource i : ds) {
          CloseableUtil.closeQuietly(i);
        }
      }
    });

    //增加退出功能
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          destroy();
        } catch (Exception ignored) {
        }
      }
    }));
  }

  @Override
  public void destroy() throws Exception {
    CloseableUtil.closeQuietly(writer);
    CloseableUtil.closeQuietly(reader);
    writer = null;
    reader = null;
  }

  private DruidDataSource buildDruidDataSource(String key, String url, String username, String password) {
    long start = System.currentTimeMillis();
    DruidDataSource ds;
    ds = new DruidDataSource();
    ds.setName(configName + '-' + key);
    ds.setUrl(url);
    ds.setUsername(username);
    ds.setPassword(password);
    try {
      ds.setFilters("stat,mergeStat,slf4j");
    } catch (Exception e) {
      LOG.error("cannot add slf4j filter with {}", url, e);
    }
    ds.setMaxActive(200);
    ds.setInitialSize(5);
    ds.setMinIdle(10);
    ds.setMaxWait(60000);
    //ds.setTimeBetweenEvictionRunsMillis(3000);
    //ds.setMinEvictableIdleTimeMillis(300000);
    ds.setValidationQuery("SELECT 'x'");
    ds.setPoolPreparedStatements(true);
    ds.setMaxPoolPreparedStatementPerConnectionSize(30);
    ds.setTestWhileIdle(true);
    ds.setTestOnReturn(false);
    ds.setTestOnBorrow(false);
    try {
      ds.init();
    } catch (Exception e) {
      LOG.error("cannot init [{}]", url, e);
    } finally {
      long cost = System.currentTimeMillis() - start;
      LOG.info("build dataSource({}) url={}, cost {}ms", ds.getName(), url, cost);
    }
    return ds;
  }

}
