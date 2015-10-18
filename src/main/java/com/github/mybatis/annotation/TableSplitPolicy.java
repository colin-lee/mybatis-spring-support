package com.github.mybatis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于类的某个函数获取表名后缀，这样可以按天或者某个字段hash分表，比较灵活，由业务自己控制
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TableSplitPolicy {
  String value() default "getPostfix"; //访问类成员函数获取表名后缀
}
