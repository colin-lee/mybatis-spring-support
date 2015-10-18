package com.github.mybatis.entity;

import java.util.Date;

public class TestDynamic extends IdEntity {
  private String group;
  private String name;
  private Date modifyDate;

  public TestDynamic() {
  }

  public TestDynamic(String group, String name) {
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

  public Date getModifyDate() {
    return modifyDate;
  }

  public void setModifyDate(Date modifyDate) {
    this.modifyDate = modifyDate;
  }
}
