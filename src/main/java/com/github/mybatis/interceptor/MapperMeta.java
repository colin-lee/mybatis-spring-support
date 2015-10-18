package com.github.mybatis.interceptor;

import org.apache.ibatis.mapping.ResultMap;

import java.util.List;

/**
 * 元信息
 *
 * Created by lirui on 2014/10/30.
 */
class MapperMeta {
  private Class<?> entity;
  private boolean fillEntity;
  private boolean fillResultMap;
  private List<ResultMap> resultMaps;

  public MapperMeta(Class<?> entity, boolean fillEntity, boolean fillResultMap, List<ResultMap> resultMaps) {
    this.entity = entity;
    this.fillEntity = fillEntity;
    this.fillResultMap = fillResultMap;
    this.resultMaps = resultMaps;
  }

  public Class<?> getEntity() {
    return entity;
  }

  public void setEntity(Class<?> entity) {
    this.entity = entity;
  }

  public boolean isFillEntity() {
    return fillEntity;
  }

  public void setFillEntity(boolean fillEntity) {
    this.fillEntity = fillEntity;
  }

  public boolean isFillResultMap() {
    return fillResultMap;
  }

  public void setFillResultMap(boolean fillResultMap) {
    this.fillResultMap = fillResultMap;
  }

  public List<ResultMap> getResultMaps() {
    return resultMaps;
  }

  public void setResultMaps(List<ResultMap> resultMaps) {
    this.resultMaps = resultMaps;
  }
}
