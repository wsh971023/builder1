package com.cuizhy.rhc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Config {

  /**
   * 主键
   */
  private long id;

  /**
   * 类型  jenkins  / git
   */
  private String type;

  /**
   * 键
   */
  private String key;

  /**
   * 值
   */
  private String value;
}
