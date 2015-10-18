package com.github.mybatis.interceptor;

import com.github.mybatis.annotation.AutoResultMap;
import com.github.mybatis.annotation.FillEntityType;
import com.github.mybatis.pagination.Page;
import com.github.mybatis.provider.CrudProvider;
import com.github.mybatis.util.CloseableUtil;
import com.github.mybatis.util.EntityUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

/**
 * 自动生成泛型resultMap，并将泛型类设置到参数中
 */
@Intercepts({@Signature(type = Executor.class, method = "query",
  args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
  @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
  @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class}),
  @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})})
public class PaginationAutoMapInterceptor extends BaseInterceptor implements Interceptor {
  private static final String GENERATE_RESULT_MAP_NAME = "GeneratedResultMap";
  private static final Map<String, Class<?>> ENTITY_CACHE = Maps.newConcurrentMap();
  private static final Map<String, MapperMeta> CACHED = Maps.newConcurrentMap();
  private static final ThreadLocal<Page> PAGE_THREAD_LOCAL = new ThreadLocal<>();
  private static final String DEFAULT_DIALECT = "mysql";
  private String dialect;

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    final String name = invocation.getMethod().getName();
    final Object target = invocation.getTarget();
    if (target instanceof StatementHandler) {
      pagination(invocation, (StatementHandler) target);
    } else if (target instanceof Executor) {
      autoMap(invocation, name);
    } else if (target instanceof ResultSetHandler) {
      Object result = invocation.proceed();
      if (result instanceof List) {
        Page page = PAGE_THREAD_LOCAL.get();
        if (page != null) {
          page.addAll((List) result);
          PAGE_THREAD_LOCAL.remove();
          return page;
        }
      }
      return result;
    }

    return invocation.proceed();
  }

  /**
   * 从传递的参数中找Page对象，并返回
   *
   * @param paramObj
   * @return
   */
  public Page findPageParameter(Object paramObj) {
    Page page = null;
    if (paramObj instanceof Page) {
      page = (Page) paramObj;
    } else if (paramObj instanceof Map) {
      Map m = (Map) paramObj;
      for (Object o : m.values()) {
        if (o instanceof Page) {
          page = (Page) o;
          break;
        }
      }
    }
    if (page != null) {
      PAGE_THREAD_LOCAL.set(page);
    }
    return page;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void autoMap(Invocation invocation, String name) throws ClassNotFoundException {
    final Object[] queryArgs = invocation.getArgs();
    final MappedStatement ms = (MappedStatement) queryArgs[0];
    final Object parameter = queryArgs[1];
    String statementId = ms.getId();
    MapperMeta meta = getMapperMeta(ms.getConfiguration(), statementId);
    if (meta.isFillEntity()) {
      // 将泛型类加入到参数中供CrudTemplate使用
      if (parameter != null) {
        Map map;
        if (parameter instanceof Map) {
          map = (HashMap) parameter;
          map.put(CrudProvider.CLASS_KEY, meta.getEntity());
        } else {
          map = new HashMap();
          map.put(CrudProvider.PARA_KEY, parameter);
          map.put(CrudProvider.CLASS_KEY, meta.getEntity());
        }
        queryArgs[1] = map;
      } else {
        queryArgs[1] = meta.getEntity();
      }
    }

    if (meta.isFillResultMap()) {
      MetaObject metaMappedStatement = getMetaObject(ms);
      metaMappedStatement.setValue("resultMaps", meta.getResultMaps());
    }

    if (name.equals("query")) {
      final RowBounds rowBounds = (RowBounds) queryArgs[2];
      if (rowBounds == null || rowBounds == RowBounds.DEFAULT) {
        Page p = findPageParameter(queryArgs[1]);
        if (p != null) {
          queryArgs[2] = new RowBounds(p.getOffset(), p.getLimit());
        }
      }
    }
  }

  private void pagination(Invocation invocation, StatementHandler target) throws SQLException {
    final MetaObject metaStatementHandler = getMetaObject(target);
    final BoundSql boundSql = target.getBoundSql();
    Page page = PAGE_THREAD_LOCAL.get();
    if (page == null) {
      page = findPageParameter(boundSql.getParameterObject());
    }
    // 如果传入的参数中有分页对象且sql语句中有select，才做分页处理
    String sql = boundSql.getSql().toLowerCase();
    if (sql.startsWith("select") && page != null) {
      // 采用物理分页后，就不需要mybatis的内存分页了，所以重置下面的两个参数
      metaStatementHandler.setValue("delegate.rowBounds.offset", RowBounds.NO_ROW_OFFSET);
      metaStatementHandler.setValue("delegate.rowBounds.limit", RowBounds.NO_ROW_LIMIT);
      // 设置分页对象里的总记录数和总页数
      Connection connection = (Connection) invocation.getArgs()[0];
      MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");
      if (page.isCountTotal()) {
        int recordsTotal = getTotalCount(sql, connection, mappedStatement, boundSql);
        page.setTotalNum(recordsTotal);
      }
      // 最后重写sql
      String pageSql = buildPageSql(sql, page);
      metaStatementHandler.setValue("delegate.boundSql.sql", pageSql);
    }
  }

  /**
   * 根据namespace查找需要的信息，避免每次反射的耗时操作
   *
   * @param conf
   * @param statementId
   * @return
   * @throws ClassNotFoundException
   */
  private MapperMeta getMapperMeta(Configuration conf, final String statementId) throws ClassNotFoundException {
    MapperMeta meta = CACHED.get(statementId);
    if (meta == null) {
      int pos = statementId.lastIndexOf('.');
      String namespace = statementId.substring(0, pos);// mapper类名
      String methodName = statementId.substring(pos + 1);

      Class<?> mapperClass;
      mapperClass = Class.forName(namespace);
      synchronized (this) {
        meta = CACHED.get(statementId);
        if (meta == null) {
          meta = buildMapperMeta(conf, namespace, methodName, mapperClass);
          CACHED.put(statementId, meta);
        }
      }
    }
    return meta;
  }

  private MapperMeta buildMapperMeta(Configuration conf, String namespace, String methodName, Class<?> mapperClass) {
    Class<?> entityClazz = null;
    List<ResultMap> resultMaps = null;
    Boolean fillEntity = Boolean.FALSE, fillResultMap = Boolean.FALSE;
    if (mapperClass != null) {
      Method method = getMethodByName(methodName, mapperClass);
      if (method != null) {
        if (method.isAnnotationPresent(AutoResultMap.class) || method.isAnnotationPresent(FillEntityType.class)) {
          FillEntityType annotation = method.getAnnotation(FillEntityType.class);
          if (annotation == null || annotation.value() == Object.class) {
            entityClazz = getEntityClass(namespace);
          } else {
            entityClazz = annotation.value();
          }
          if (entityClazz != Object.class) {
            fillEntity = Boolean.TRUE;
          }

          if (method.isAnnotationPresent(AutoResultMap.class)) {
            // 重写resultMaps属性
            resultMaps = getResultMap(namespace, entityClazz, conf);
            fillResultMap = Boolean.TRUE;
          }
        }
      }
    }

    return new MapperMeta(entityClazz, fillEntity, fillResultMap, resultMaps);
  }

  private Method getMethodByName(String methodName, Class<?> entityClazz) {
    Method[] methods = entityClazz.getMethods();
    Method method = null;
    for (Method i : methods) {
      if (methodName.equals(i.getName())) {
        method = i;
        break;
      }
    }
    return method;
  }

  private Class<?> getEntityClass(String namespace) {
    Class<?> entityClazz = ENTITY_CACHE.get(namespace);
    if (entityClazz == null) {
      try {
        Type[] genTypes = Class.forName(namespace).getGenericInterfaces();
        if (genTypes.length == 0) {
          return null;
        }
        Type type = genTypes[0];
        if (!(type instanceof ParameterizedType)) {
          return null;
        }
        Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
        logger.info("namespace:{}, EntityClass: {}", namespace, actualTypeArguments);
        entityClazz = (Class<?>) actualTypeArguments[0];
      } catch (ClassNotFoundException e) {
        logger.error("getEntityClass({})", namespace, e);
        entityClazz = Object.class;
      }
      ENTITY_CACHE.put(namespace, entityClazz);
    }
    return entityClazz;
  }

  /**
   * 生成泛型对应的ResultMap
   *
   * @param namespace
   * @param clazz
   * @param conf
   * @return
   */
  private List<ResultMap> getResultMap(String namespace, Class<?> clazz, Configuration conf) {
    String dynamicResultMapId = namespace + "." + GENERATE_RESULT_MAP_NAME;
    ResultMap dynamicResultMap = buildResultMap(dynamicResultMapId, clazz, conf);
    List<ResultMap> resultMaps = Lists.newArrayList();
    resultMaps.add(dynamicResultMap);
    return Collections.unmodifiableList(resultMaps);
  }

  /**
   * 构建ResultMap对象
   *
   * @param id
   * @param clazz
   * @param configuration
   * @return
   */
  private ResultMap buildResultMap(String id, Class<?> clazz, Configuration configuration) {
    // 判断是否已经存在缓存里
    if (configuration.hasResultMap(id)) {
      return configuration.getResultMap(id);
    }
    List<ResultMapping> resultMappings = Lists.newArrayList();
    Map<String, Field> columns = EntityUtil.getFields(clazz);
    for (Map.Entry<String, Field> column : columns.entrySet()) {
      Field field = column.getValue();
      String fieldName = field.getName();
      Class<?> columnTypeClass = resolveResultJavaType(clazz, fieldName, null);
      List<ResultFlag> flags = Lists.newArrayList();
      if (field.isAnnotationPresent(Id.class)) {
        flags.add(ResultFlag.ID);
      }
      String columnName = column.getKey();
      resultMappings.add(buildResultMapping(configuration, fieldName, columnName, columnTypeClass, flags));
    }

    // 构建ResultMap
    ResultMap.Builder resultMapBuilder = new ResultMap.Builder(configuration, id, clazz, resultMappings);
    ResultMap rm = resultMapBuilder.build();
    // 放到缓存中
    configuration.addResultMap(rm);
    return rm;
  }

  /**
   * 构建ResultMapping对象
   *
   * @param configuration
   * @param property
   * @param column
   * @param javaType
   * @param flags
   * @return
   */
  private ResultMapping buildResultMapping(Configuration configuration, String property, String column, Class<?> javaType, List<ResultFlag> flags) {
    ResultMapping.Builder builder = new ResultMapping.Builder(configuration, property, column, javaType);
    builder.flags(flags == null ? new ArrayList<ResultFlag>() : flags);
    builder.composites(new ArrayList<ResultMapping>());
    builder.notNullColumns(new HashSet<String>());
    return builder.build();
  }

  /**
   * copy from mybatis sourceCode
   *
   * @param resultType
   * @param property
   * @param javaType
   * @return
   */
  private Class<?> resolveResultJavaType(Class<?> resultType, String property, Class<?> javaType) {
    if (javaType == null && property != null) {
      try {
        MetaClass metaResultType = MetaClass.forClass(resultType, REFLECTOR_FACTORY);
        javaType = metaResultType.getSetterType(property);
      } catch (Exception ignored) {
        // ignore, following null check statement will deal with the
        // situation
      }
    }
    if (javaType == null) {
      javaType = Object.class;
    }
    return javaType;
  }

  /**
   * 获取总记录数
   *
   * @param sql      原始sql语句
   * @param conn
   * @param ms
   * @param boundSql
   * @return
   * @throws SQLException
   */
  private int getTotalCount(String sql, Connection conn, MappedStatement ms, BoundSql boundSql) throws SQLException {
    int start = sql.indexOf("from");
    if (start == -1) {
      throw new RuntimeException("statement has no 'from' keyword");
    }
    int stop = sql.indexOf("order by");
    if (stop == -1) {
      stop = sql.length();
    }

    String countSql = "select count(0) " + sql.substring(start, stop);
    BoundSql countBoundSql = new BoundSql(ms.getConfiguration(), countSql, boundSql.getParameterMappings(), boundSql.getParameterObject());
    ParameterHandler parameterHandler = new DefaultParameterHandler(ms, boundSql.getParameterObject(), countBoundSql);
    PreparedStatement stmt = null;
    ResultSet rs = null;
    int count = 0;
    try {
      stmt = conn.prepareStatement(countSql);
      // 通过parameterHandler给PreparedStatement对象设置参数
      parameterHandler.setParameters(stmt);
      rs = stmt.executeQuery();
      if (rs.next()) {
        count = rs.getInt(1);
      }
    } finally {
      CloseableUtil.closeQuietly(rs);
      CloseableUtil.closeQuietly(stmt);
    }
    return count;
  }

  /**
   * 生成特定数据库的分页语句
   *
   * @param sql
   * @param page
   * @return
   */
  private String buildPageSql(String sql, Page page) {
    if (page == null || dialect == null || dialect.equals("")) {
      return sql;
    }
    StringBuilder sb = new StringBuilder();
    int startRow = page.getOffset();

    if ("mysql".equals(dialect)) {
      sb.append(sql);
      sb.append(" limit ").append(startRow).append(",").append(page.getLimit());
    } else if ("hsqldb".equals(dialect)) {
      sb.append("select limit ");
      sb.append(startRow);
      sb.append(" ");
      sb.append(page.getLimit());
      sb.append(" ");
      sb.append(sql.substring(6));
    } else if ("oracle".equals(dialect)) {
      sb.append("select * from (select tmp_tb.*,ROWNUM row_id from (");
      sb.append(sql);
      sb.append(")  tmp_tb where ROWNUM<=");
      sb.append(startRow + page.getLimit());
      sb.append(") where row_id>");
      sb.append(startRow);
    } else {
      throw new IllegalArgumentException("SelectInterceptor error:does not support " + dialect);
    }
    return sb.toString();
  }

  public String getDialect() {
    return dialect;
  }

  public void setDialect(String dialect) {
    this.dialect = dialect;
  }

  @Override
  public Object plugin(Object target) {
    if (target instanceof Executor || target instanceof StatementHandler || target instanceof ResultSetHandler) {
      return Plugin.wrap(target, this);
    } else {
      return target;
    }
  }

  @Override
  public void setProperties(Properties properties) {
    String dialect = properties.getProperty("dialect");
    if (Strings.isNullOrEmpty(dialect)) {
      dialect = DEFAULT_DIALECT;
    }
    setDialect(dialect);
  }
}
