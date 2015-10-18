package com.github.mybatis.datatables.compatible;

import java.io.Serializable;
import java.util.List;

/**
 * datatables响应
 * Created by lirui on 15/2/8.
 */
public class DataTablesResponse<T> implements Serializable {
  private int iTotalRecords;
  private int iTotalDisplayRecords;
  private String sEcho;
  private String sColumns;
  private List<T> aaData;

  public int getiTotalRecords() {
    return iTotalRecords;
  }

  public void setiTotalRecords(int iTotalRecords) {
    this.iTotalRecords = iTotalRecords;
  }

  public int getiTotalDisplayRecords() {
    return iTotalDisplayRecords;
  }

  public void setiTotalDisplayRecords(int iTotalDisplayRecords) {
    this.iTotalDisplayRecords = iTotalDisplayRecords;
  }

  public String getsEcho() {
    return sEcho;
  }

  public void setsEcho(String sEcho) {
    this.sEcho = sEcho;
  }

  public String getsColumns() {
    return sColumns;
  }

  public void setsColumns(String sColumns) {
    this.sColumns = sColumns;
  }

  public List<T> getAaData() {
    return aaData;
  }

  public void setAaData(List<T> aaData) {
    this.aaData = aaData;
  }
}
