package com.github.mybatis.pagination;

import java.util.ArrayList;

/**
 * 翻页信息
 *
 * Created by lirui on 2014/10/30.
 */
public class Page<T> extends ArrayList<T> {
  /**
   * 页码从1开始
   */
  private int pageNo = 1;
  /**
   * 一页包含元素个数
   */
  private int pageSize = 10;

  /**
   * 默认计算总行数
   */
  private boolean countTotal = true;

  /**
   * 总页码信息
   */
  private int totalNum;

  public Page() {
  }

  public Page(int pageNo, int pageSize) {
    this(pageNo, pageSize, false);
  }

  public Page(int pageNo, int pageSize, boolean countTotal) {
    super(pageSize);
    this.pageNo = pageNo;
    this.pageSize = pageSize;
    this.countTotal = countTotal;
  }

  public int getPageNo() {
    return pageNo;
  }

  public void setPageNo(int pageNo) {
    this.pageNo = pageNo;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public boolean isCountTotal() {
    return countTotal;
  }

  public void setCountTotal(boolean countTotal) {
    this.countTotal = countTotal;
  }

  public int getTotalNum() {
    return totalNum;
  }

  public void setTotalNum(int totalNum) {
    this.totalNum = totalNum;
  }

  public int getOffset() {
    int offset = (pageNo - 1) * pageSize;
    return offset < 0 ? 0 : offset;
  }

  public int getLimit() {
    return pageSize;
  }

  /**
   * 翻页总数
   *
   * @return
   */
  public int getTotalPage() {
    int i = totalNum % pageSize;
    int j = totalNum / pageSize;
    return i == 0 ? j : j + 1;
  }

}
