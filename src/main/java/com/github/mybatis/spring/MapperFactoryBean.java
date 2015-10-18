package com.github.mybatis.spring;

/*
 *    Copyright 2010-2012 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import com.github.mybatis.entity.IdEntity;
import com.github.trace.TraceContext;
import com.github.trace.TraceRecorder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.Reflection;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;

import static org.springframework.util.Assert.notNull;

/**
 * BeanFactory that enables injection of MyBatis mapper interfaces. It can be set up with a
 * SqlSessionFactory or a pre-configured SqlSessionTemplate.
 *
 * Sample configuration:
 *
 * <pre class="code">
 * {@code
 * <bean id="baseMapper" class="org.mybatis.spring.mapper.MapperFactoryBean" abstract="true" lazy-init="true">
 * <property name="sqlSessionFactory" ref="sqlSessionFactory" />
 * </bean>
 *
 * <bean id="oneMapper" parent="baseMapper">
 * <property name="mapperInterface" value="my.package.MyMapperInterface" />
 * </bean>
 *
 * <bean id="anotherMapper" parent="baseMapper">
 * <property name="mapperInterface" value="my.package.MyAnotherMapperInterface" />
 * </bean>
 * }
 * </pre>
 *
 * Note that this factory can only inject <em>interfaces</em>, not concrete classes.
 *
 * @author Eduardo Macarron
 * @version $Id$
 * @see org.mybatis.spring.SqlSessionTemplate
 */
public class MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T> {
  private static final Logger LOG = LoggerFactory.getLogger(MapperFactoryBean.class);
  private static final TimeZone CHINA_ZONE = TimeZone.getTimeZone("GMT+08:00");
  private static final Locale CHINA_LOCALE = Locale.CHINA;
  private static final Set<String> NAMES = ImmutableSet.of("Boolean", "Character", "Byte", "Short", "Long", "Integer", "Byte", "Float", "Double", "Void", "String");
  private Class<T> mapperInterface;
  private String iface;

  private boolean addToConfig = true;

  /**
   * Sets the mapper interface of the MyBatis mapper
   *
   * @param mapperInterface class of the interface
   */
  public void setMapperInterface(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
    this.iface = mapperInterface.getSimpleName();
  }

  /**
   * If addToConfig is false the mapper will not be added to MyBatis. This means
   * it must have been included in mybatis-config.xml.
   *
   * If it is true, the mapper will be added to MyBatis in the case it is not already
   * registered.
   *
   * By default addToCofig is true.
   *
   * @param addToConfig
   */
  public void setAddToConfig(boolean addToConfig) {
    this.addToConfig = addToConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void checkDaoConfig() {
    super.checkDaoConfig();
    notNull(this.mapperInterface, "Property 'mapperInterface' is required");

    Configuration configuration = getSqlSession().getConfiguration();
    if (this.addToConfig && !configuration.hasMapper(this.mapperInterface)) {
      try {
        configuration.addMapper(this.mapperInterface);
      } catch (Throwable t) {
        logger.error("Error while adding the mapper '" + this.mapperInterface + "' to configuration.", t);
        throw new IllegalArgumentException(t);
      } finally {
        ErrorContext.instance().reset();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public T getObject() throws Exception {
    final T mapper = getSqlSession().getMapper(this.mapperInterface);

    return Reflection.newProxy(this.mapperInterface, new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.currentTimeMillis();
        TraceContext rpc = TraceContext.get();
        String parameters = getParameters(args);
        String iface = MapperFactoryBean.this.iface;
        rpc.reset().inc().setStamp(start).setIface(iface).setMethod(method.getName()).setParameter(parameters);
        try {
          return method.invoke(mapper, args);
        } catch (Exception e) {
          rpc.setFail(true).setReason(e.getMessage());
          LOG.error("{}.{}({})", iface, method.getName(), parameters, e);
          throw e;
        } finally {
          rpc.setCost(System.currentTimeMillis() - start);
          TraceRecorder.getInstance().post(rpc);
        }
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  public Class<T> getObjectType() {
    return this.mapperInterface;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isSingleton() {
    return false;
  }

  /**
   * 函数参数信息
   *
   * @param args 参数列表
   * @return 格式化输出
   */
  protected String getParameters(Object[] args) {
    if (args == null)
      return "";
    StringBuilder sbd = new StringBuilder();
    if (args.length > 0) {
      for (Object i : args) {
        if (i == null) {
          sbd.append("null");
        } else {
          Class clz = i.getClass();
          if (isPrimitive(clz)) {
            sbd.append(evalPrimitive(i));
          } else if (clz.isArray()) {
            evalArray(i, sbd);
          } else if (Collection.class.isAssignableFrom(clz)) {
            Object[] arr = ((Collection<?>) i).toArray();
            evalArray(arr, sbd);
          } else if (i instanceof Date) {
            sbd.append('"').append(formatYmdHis(((Date) i))).append('"');
          } else if (i instanceof IdEntity) {
            sbd.append(i.getClass().getSimpleName()).append("[id=").append(((IdEntity) i).getId()).append(']');
          }else {
            sbd.append(clz.getSimpleName()).append(":OBJ");
          }
        }
        sbd.append(',');
      }
      sbd.setLength(sbd.length() - 1);
    }
    return sbd.toString();
  }

  private static boolean isPrimitive(Class clz) {
    return clz.isPrimitive() || NAMES.contains(clz.getSimpleName());
  }

  private String evalPrimitive(Object obj) {
    String s = String.valueOf(obj);
    if (s.length() > 32) {
      return s.substring(0, 32) + "...";
    }
    return s;
  }

  private void evalArray(Object arr, StringBuilder sbd) {
    int sz = Array.getLength(arr);
    if (sz == 0) {
      sbd.append("[]");
      return;
    }
    Class<?> clz = Array.get(arr, 0).getClass();
    if (clz == Byte.class) {
      sbd.append("Byte[").append(sz).append(']');
      return;
    }
    if (isPrimitive(clz)) {
      sbd.append('[');
      int len = Math.min(sz, 10);
      for (int i = 0; i < len; i++) {
        Object obj = Array.get(arr, i);
        if (isPrimitive(obj.getClass())) {
          sbd.append(evalPrimitive(obj));
        } else {
          sbd.append(obj.getClass().getSimpleName()).append(":OBJ");
        }
        sbd.append(',');
      }
      if (sz > 10) {
        sbd.append(",...,len=").append(sz);
      }
      if (sbd.charAt(sbd.length() - 1) == ',') {
        sbd.setCharAt(sbd.length() - 1, ']');
      } else {
        sbd.append(']');
      }
    } else {
      sbd.append("[len=").append(sz).append(']');
    }
  }

  /**
   * 构造时间的显示，带上时分秒的信息，如 2013-06-11 03:14:25
   *
   * @param date 时间
   * @return 字符串表示
   */
  private String formatYmdHis(Date date) {
    Calendar ca = Calendar.getInstance(CHINA_ZONE, CHINA_LOCALE);
    ca.setTimeInMillis(date.getTime());
    StringBuilder sbd = new StringBuilder();
    sbd.append(ca.get(Calendar.YEAR)).append('-');
    int month = 1 + ca.get(Calendar.MONTH);
    if (month < 10) {
      sbd.append('0');
    }
    sbd.append(month).append('-');
    int day = ca.get(Calendar.DAY_OF_MONTH);
    if (day < 10) {
      sbd.append('0');
    }
    sbd.append(day).append(' ');
    int hour = ca.get(Calendar.HOUR_OF_DAY);
    if (hour < 10) {
      sbd.append('0');
    }
    sbd.append(hour).append(':');
    int minute = ca.get(Calendar.MINUTE);
    if (minute < 10) {
      sbd.append('0');
    }
    sbd.append(minute).append(':');
    int second = ca.get(Calendar.SECOND);
    if (second < 10) {
      sbd.append('0');
    }
    sbd.append(second);
    return sbd.toString();
  }

  protected Object findDefault(Method method) {
    Class<?> clz = method.getReturnType();
    if (clz == String.class) {
      return "";
    } else if (clz == Long.class || clz == Double.class) {
      return 0L;
    } else if (clz == Boolean.class) {
      return Boolean.FALSE;
    } else if (clz.isArray()) {
      return new byte[0];
    } else if (clz.isAssignableFrom(Set.class)) {
      return ImmutableSet.of();
    } else if (clz.isAssignableFrom(List.class)) {
      return ImmutableList.of();
    } else if (clz.isAssignableFrom(Map.class)) {
      return ImmutableMap.of();
    } else {
      return null;
    }
  }
}
