package com.github.mybatis.interceptor;

import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 拦截器基类
 */

public abstract class BaseInterceptor {
  static final ReflectorFactory REFLECTOR_FACTORY = new DefaultReflectorFactory();
  static final DefaultObjectFactory OBJECT_FACTORY = new DefaultObjectFactory();
  static final DefaultObjectWrapperFactory OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
  protected Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * 获取被拦截的对象(MetaObject包装)
   *
   * @param obj
   * @return
   */
  protected MetaObject getMetaObject(Object obj) {
    ObjectFactory objectFactory = OBJECT_FACTORY;
    ObjectWrapperFactory objectWrapperFactory = OBJECT_WRAPPER_FACTORY;
    MetaObject metaStatementHandler = MetaObject.forObject(obj, objectFactory, objectWrapperFactory, REFLECTOR_FACTORY);
    // 由于目标类可能被多个拦截器拦截，从而形成多次代理，通过以下循环找出原始代理
    while (metaStatementHandler.hasGetter("h")) {
      Object object = metaStatementHandler.getValue("h");
      metaStatementHandler = MetaObject.forObject(object, objectFactory, objectWrapperFactory, REFLECTOR_FACTORY);
    }
    // 得到原始代理对象的目标类，即StatementHandler实现类
    if (metaStatementHandler.hasGetter("target")) {
      Object object = metaStatementHandler.getValue("target");
      metaStatementHandler = MetaObject.forObject(object, objectFactory, objectWrapperFactory, REFLECTOR_FACTORY);
    }
    return metaStatementHandler;
  }
}
