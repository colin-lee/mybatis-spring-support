package com.github.mybatis.datatables.compatible;

import java.io.Serializable;

/**
 * datatables请求解析
 * Created by lirui on 15/2/8.
 */
public class DataTablesRequest implements Serializable {
  private String sEcho;
  private String sColumns;
  private int iDisplayStart;
  private int iDisplayLength;
  private int iColumns;
  private String sSearch;
  private boolean bRegex;
  private int iSortCol_0;
  private String sSortDir_0;

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

  public int getiDisplayStart() {
    return iDisplayStart;
  }

  public void setiDisplayStart(int iDisplayStart) {
    this.iDisplayStart = iDisplayStart;
  }

  public int getiDisplayLength() {
    return iDisplayLength;
  }

  public void setiDisplayLength(int iDisplayLength) {
    this.iDisplayLength = iDisplayLength;
  }

  public int getiColumns() {
    return iColumns;
  }

  public void setiColumns(int iColumns) {
    this.iColumns = iColumns;
  }

  public String getsSearch() {
    return sSearch;
  }

  public void setsSearch(String sSearch) {
    this.sSearch = sSearch;
  }

  public boolean isbRegex() {
    return bRegex;
  }

  public void setbRegex(boolean bRegex) {
    this.bRegex = bRegex;
  }

  public int getiSortCol_0() {
    return iSortCol_0;
  }

  public void setiSortCol_0(int iSortCol_0) {
    this.iSortCol_0 = iSortCol_0;
  }

  public String getsSortDir_0() {
    return sSortDir_0;
  }

  public void setsSortDir_0(String sSortDir_0) {
    this.sSortDir_0 = sSortDir_0;
  }
}
