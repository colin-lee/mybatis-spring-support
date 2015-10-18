package com.github.mybatis.entity;

import javax.persistence.Table;
import javax.persistence.Transient;

@Table(name = "test_page")
public class TestPage extends IdEntity {
  private String group;
  private String name;

  @Transient
  private int ignore;

  public TestPage() {
  }

  public TestPage(String group, String name) {
    this.group = group;
    this.name = name;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getIgnore() {
    return ignore;
  }

  public void setIgnore(int ignore) {
    this.ignore = ignore;
  }
}
