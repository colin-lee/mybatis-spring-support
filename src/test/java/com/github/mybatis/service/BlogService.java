package com.github.mybatis.service;

import com.github.mybatis.mapper.BlogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 测试autowire
 * Created by lirui on 15/1/11.
 */
@Service
public class BlogService {
  @Autowired
  private BlogMapper mapper;
}
