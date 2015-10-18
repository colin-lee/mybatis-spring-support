package com.github.mybatis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解到类上，指明表名和列名的映射规则
 * Created by lirui on 2015/03/05 下午12:09.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NameMappingPolicy {
  String value() default "mapCamelCaseToUnderscore";
}
