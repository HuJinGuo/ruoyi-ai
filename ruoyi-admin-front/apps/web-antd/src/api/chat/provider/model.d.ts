import type { BaseEntity, PageQuery } from '#/api/common';

export interface ProviderVO {
  /**
   * 主键
   */
  id: number | string;

  /**
   * 厂商名称
   */
  providerName: string;

  /**
   * 厂商编码
   */
  providerCode: string;

  /**
   * 厂商图标
   */
  providerIcon: string;

  /**
   * 厂商描述
   */
  providerDesc: string;

  /**
   * API地址
   */
  apiHost: string;

  /**
   * 状态（0正常 1停用）
   */
  status: string;

  /**
   * 排序
   */
  sortOrder: number;

  /**
   * 备注
   */
  remark: string;

  /**
   * 更新IP
   */
  updateIp: string;
}

export interface ProviderForm extends BaseEntity {
  /**
   * 主键
   */
  id?: number | string;

  /**
   * 厂商名称
   */
  providerName?: string;

  /**
   * 厂商编码
   */
  providerCode?: string;

  /**
   * 厂商图标
   */
  providerIcon?: string;

  /**
   * 厂商描述
   */
  providerDesc?: string;

  /**
   * API地址
   */
  apiHost?: string;

  /**
   * 状态（0正常 1停用）
   */
  status?: string;

  /**
   * 排序
   */
  sortOrder?: number;

  /**
   * 备注
   */
  remark?: string;

  /**
   * 更新IP
   */
  updateIp?: string;
}

export interface ProviderQuery extends PageQuery {
  /**
   * 厂商名称
   */
  providerName?: string;

  /**
   * 厂商编码
   */
  providerCode?: string;

  /**
   * 厂商图标
   */
  providerIcon?: string;

  /**
   * 厂商描述
   */
  providerDesc?: string;

  /**
   * API地址
   */
  apiHost?: string;

  /**
   * 状态（0正常 1停用）
   */
  status?: string;

  /**
   * 排序
   */
  sortOrder?: number;

  /**
   * 更新IP
   */
  updateIp?: string;

  /**
   * 日期范围参数
   */
  params?: any;
}
