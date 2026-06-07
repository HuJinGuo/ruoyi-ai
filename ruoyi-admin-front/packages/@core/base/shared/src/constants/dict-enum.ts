export const DictEnum = {
  SYS_COMMON_STATUS: 'sys_common_status',
  SYS_DEVICE_TYPE: 'sys_device_type', // 设备类型
  SYS_GRANT_TYPE: 'sys_grant_type', // 授权类型
  SYS_NORMAL_DISABLE: 'sys_normal_disable',
  SYS_NOTICE_STATUS: 'sys_notice_status', // 通知状态
  SYS_NOTICE_TYPE: 'sys_notice_type', // 通知类型
  SYS_OPER_TYPE: 'sys_oper_type', // 操作类型
  SYS_OSS_ACCESS_POLICY: 'oss_access_policy', // oss权限桶类型
  SYS_SHOW_HIDE: 'sys_show_hide', // 显示状态
  SYS_USER_SEX: 'sys_user_sex', // 性别
  SYS_YES_NO: 'sys_yes_no', // 是否
  WF_BUSINESS_STATUS: 'wf_business_status', // 业务状态
  WF_FORM_TYPE: 'wf_form_type', // 表单类型
  WF_TASK_STATUS: 'wf_task_status', // 任务状态
  SYS_MODEL_BILLING: 'sys_model_billing', // 计费方式
  CHAT_MODEL_CATEGORY: 'chat_model_category', // 模型分类
  CRM_CONTRACT_STATUS: 'crm_contract_status', // CRM合同状态
  CRM_CUSTOMER_LEVEL: 'crm_customer_level', // CRM客户等级
  CRM_CUSTOMER_STATUS: 'crm_customer_status', // CRM客户状态
  CRM_CUSTOMER_TYPE: 'crm_customer_type', // CRM客户类型
  CRM_DECISION_ROLE: 'crm_decision_role', // CRM决策角色
  CRM_FOLLOW_METHOD: 'crm_follow_method', // CRM跟进方式
  CRM_FOLLOW_RESULT: 'crm_follow_result', // CRM跟进结果
  CRM_OPPORTUNITY_SOURCE: 'crm_opportunity_source', // CRM商机来源
  CRM_OPPORTUNITY_STAGE: 'crm_opportunity_stage', // CRM商机阶段
  CRM_PAYMENT_STATUS: 'crm_payment_status', // CRM回款状态
  CRM_QUOTE_STATUS: 'crm_quote_status', // CRM报价状态
  XTP_ENABLE_STATUS: 'xtp_enable_status', // XTP启停状态
  XTP_ENGINEERING_MATERIAL_STATUS: 'xtp_engineering_material_status', // XTP工程物料状态
  XTP_ISSUE_STATUS: 'xtp_issue_status', // XTP发料状态
  XTP_MATERIAL_CATEGORY: 'xtp_material_category', // XTP物料分类
  XTP_PURCHASE_ORDER_STATUS: 'xtp_purchase_order_status', // XTP采购订单状态
  XTP_PURCHASE_REQUEST_STATUS: 'xtp_purchase_request_status', // XTP采购需求状态
  XTP_RECEIPT_STATUS: 'xtp_receipt_status', // XTP收料状态
  XTP_STAGE_STATUS: 'xtp_stage_status', // XTP阶段状态
  XTP_SUPPLIER_LEVEL: 'xtp_supplier_level', // XTP供应商等级
  XTP_SUPPLIER_STATUS: 'xtp_supplier_status', // XTP供应商状态
  XTP_WORK_ORDER_STATUS: 'xtp_work_order_status', // XTP工单状态
} as const;

export type DictEnumKey = keyof typeof DictEnum;
