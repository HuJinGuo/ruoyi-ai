-- XTP-CRM 客户中心表结构、字典与菜单
-- 执行环境：PostgreSQL

CREATE TABLE IF NOT EXISTS "crm_customer" (
  "customer_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "name" varchar(120) NOT NULL,
  "short_name" varchar(80),
  "code" varchar(64) NOT NULL,
  "type" varchar(32),
  "level" varchar(16),
  "industry" varchar(80),
  "province" varchar(50),
  "city" varchar(50),
  "district" varchar(50),
  "address" varchar(255),
  "website" varchar(255),
  "status" varchar(32) DEFAULT 'potential',
  "scale" jsonb,
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  "remark" varchar(500),
  PRIMARY KEY ("customer_id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_crm_customer_tenant_code" ON "crm_customer" ("tenant_id", "code");
CREATE INDEX IF NOT EXISTS "idx_crm_customer_name" ON "crm_customer" ("name");
CREATE INDEX IF NOT EXISTS "idx_crm_customer_status" ON "crm_customer" ("status");

CREATE TABLE IF NOT EXISTS "crm_contact" (
  "contact_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "customer_id" bigint NOT NULL,
  "name" varchar(64) NOT NULL,
  "phone" varchar(32),
  "email" varchar(120),
  "wechat" varchar(80),
  "position" varchar(80),
  "department" varchar(80),
  "decision_role" varchar(32),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  "remark" varchar(500),
  PRIMARY KEY ("contact_id")
);
CREATE INDEX IF NOT EXISTS "idx_crm_contact_customer" ON "crm_contact" ("customer_id");
CREATE INDEX IF NOT EXISTS "idx_crm_contact_name" ON "crm_contact" ("name");
CREATE INDEX IF NOT EXISTS "idx_crm_contact_phone" ON "crm_contact" ("phone");

CREATE TABLE IF NOT EXISTS "crm_opportunity" (
  "opportunity_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "customer_id" bigint NOT NULL,
  "contact_id" bigint,
  "name" varchar(160) NOT NULL,
  "estimated_amount" numeric(18,2),
  "estimated_close_date" date,
  "source" varchar(32),
  "stage" varchar(32),
  "success_rate" numeric(5,2),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  "remark" varchar(500),
  PRIMARY KEY ("opportunity_id")
);
CREATE INDEX IF NOT EXISTS "idx_crm_opportunity_customer" ON "crm_opportunity" ("customer_id");
CREATE INDEX IF NOT EXISTS "idx_crm_opportunity_contact" ON "crm_opportunity" ("contact_id");
CREATE INDEX IF NOT EXISTS "idx_crm_opportunity_stage" ON "crm_opportunity" ("stage");

CREATE TABLE IF NOT EXISTS "crm_follow_record" (
  "follow_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "opportunity_id" bigint,
  "customer_id" bigint NOT NULL,
  "contact_id" bigint,
  "follow_time" timestamp NOT NULL,
  "follow_method" varchar(32),
  "content" text NOT NULL,
  "result" varchar(32),
  "next_follow_time" timestamp,
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  "remark" varchar(500),
  PRIMARY KEY ("follow_id")
);
CREATE INDEX IF NOT EXISTS "idx_crm_follow_opportunity" ON "crm_follow_record" ("opportunity_id");
CREATE INDEX IF NOT EXISTS "idx_crm_follow_customer" ON "crm_follow_record" ("customer_id");
CREATE INDEX IF NOT EXISTS "idx_crm_follow_time" ON "crm_follow_record" ("follow_time");

CREATE TABLE IF NOT EXISTS "crm_quote" (
  "quote_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "opportunity_id" bigint,
  "customer_id" bigint NOT NULL,
  "version" int DEFAULT 1,
  "total_amount" numeric(18,2) DEFAULT 0.00,
  "status" varchar(32) DEFAULT 'draft',
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  "remark" varchar(500),
  PRIMARY KEY ("quote_id")
);
CREATE INDEX IF NOT EXISTS "idx_crm_quote_opportunity" ON "crm_quote" ("opportunity_id");
CREATE INDEX IF NOT EXISTS "idx_crm_quote_customer" ON "crm_quote" ("customer_id");
CREATE INDEX IF NOT EXISTS "idx_crm_quote_status" ON "crm_quote" ("status");

CREATE TABLE IF NOT EXISTS "crm_contract" (
  "contract_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "opportunity_id" bigint,
  "customer_id" bigint NOT NULL,
  "quote_id" bigint,
  "name" varchar(160) NOT NULL,
  "amount" numeric(18,2) DEFAULT 0.00,
  "signed_date" date,
  "delivery_date" date,
  "status" varchar(32) DEFAULT 'draft',
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  "remark" varchar(500),
  PRIMARY KEY ("contract_id")
);
CREATE INDEX IF NOT EXISTS "idx_crm_contract_opportunity" ON "crm_contract" ("opportunity_id");
CREATE INDEX IF NOT EXISTS "idx_crm_contract_customer" ON "crm_contract" ("customer_id");
CREATE INDEX IF NOT EXISTS "idx_crm_contract_quote" ON "crm_contract" ("quote_id");
CREATE INDEX IF NOT EXISTS "idx_crm_contract_status" ON "crm_contract" ("status");

CREATE TABLE IF NOT EXISTS "crm_payment_plan" (
  "payment_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "contract_id" bigint,
  "opportunity_id" bigint,
  "customer_id" bigint NOT NULL,
  "stage_name" varchar(80) NOT NULL,
  "amount" numeric(18,2) DEFAULT 0.00,
  "planned_date" date,
  "status" varchar(32) DEFAULT 'not_due',
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  "remark" varchar(500),
  PRIMARY KEY ("payment_id")
);
CREATE INDEX IF NOT EXISTS "idx_crm_payment_contract" ON "crm_payment_plan" ("contract_id");
CREATE INDEX IF NOT EXISTS "idx_crm_payment_customer" ON "crm_payment_plan" ("customer_id");
CREATE INDEX IF NOT EXISTS "idx_crm_payment_status" ON "crm_payment_plan" ("status");
CREATE INDEX IF NOT EXISTS "idx_crm_payment_planned_date" ON "crm_payment_plan" ("planned_date");

INSERT INTO "sys_dict_type" ("dict_id", "tenant_id", "dict_name", "dict_type", "create_dept", "create_by", "create_time", "remark") VALUES
(7001, '000000', 'CRM客户类型', 'crm_customer_type', 103, 1, '2026-06-07 00:00:00', 'CRM客户类型'),
(7002, '000000', 'CRM客户等级', 'crm_customer_level', 103, 1, '2026-06-07 00:00:00', 'CRM客户等级'),
(7003, '000000', 'CRM客户状态', 'crm_customer_status', 103, 1, '2026-06-07 00:00:00', 'CRM客户状态'),
(7004, '000000', 'CRM决策角色', 'crm_decision_role', 103, 1, '2026-06-07 00:00:00', 'CRM联系人决策角色'),
(7005, '000000', 'CRM商机来源', 'crm_opportunity_source', 103, 1, '2026-06-07 00:00:00', 'CRM商机来源'),
(7006, '000000', 'CRM商机阶段', 'crm_opportunity_stage', 103, 1, '2026-06-07 00:00:00', 'CRM商机阶段'),
(7007, '000000', 'CRM跟进方式', 'crm_follow_method', 103, 1, '2026-06-07 00:00:00', 'CRM跟进方式'),
(7008, '000000', 'CRM跟进结果', 'crm_follow_result', 103, 1, '2026-06-07 00:00:00', 'CRM跟进结果'),
(7009, '000000', 'CRM报价状态', 'crm_quote_status', 103, 1, '2026-06-07 00:00:00', 'CRM报价状态'),
(7010, '000000', 'CRM合同状态', 'crm_contract_status', 103, 1, '2026-06-07 00:00:00', 'CRM合同状态'),
(7011, '000000', 'CRM回款状态', 'crm_payment_status', 103, 1, '2026-06-07 00:00:00', 'CRM回款状态')
ON CONFLICT ("dict_id") DO NOTHING;

INSERT INTO "sys_dict_data" ("dict_code", "tenant_id", "dict_sort", "dict_label", "dict_value", "dict_type", "css_class", "list_class", "is_default", "create_dept", "create_by", "create_time", "remark") VALUES
(70001, '000000', 1, '终端客户', 'end_customer', 'crm_customer_type', '', 'primary', 'Y', 103, 1, '2026-06-07 00:00:00', '终端客户'),
(70002, '000000', 2, '集成商', 'integrator', 'crm_customer_type', '', 'info', 'N', 103, 1, '2026-06-07 00:00:00', '集成商'),
(70003, '000000', 3, '代理商', 'agent', 'crm_customer_type', '', 'warning', 'N', 103, 1, '2026-06-07 00:00:00', '代理商'),
(70004, '000000', 4, '供应商兼客户', 'supplier_customer', 'crm_customer_type', '', 'success', 'N', 103, 1, '2026-06-07 00:00:00', '供应商兼客户'),
(70005, '000000', 1, 'A', 'A', 'crm_customer_level', '', 'success', 'Y', 103, 1, '2026-06-07 00:00:00', 'A级客户'),
(70006, '000000', 2, 'B', 'B', 'crm_customer_level', '', 'primary', 'N', 103, 1, '2026-06-07 00:00:00', 'B级客户'),
(70007, '000000', 3, 'C', 'C', 'crm_customer_level', '', 'warning', 'N', 103, 1, '2026-06-07 00:00:00', 'C级客户'),
(70008, '000000', 4, 'D', 'D', 'crm_customer_level', '', 'default', 'N', 103, 1, '2026-06-07 00:00:00', 'D级客户'),
(70009, '000000', 1, '潜在', 'potential', 'crm_customer_status', '', 'warning', 'Y', 103, 1, '2026-06-07 00:00:00', '潜在客户'),
(70010, '000000', 2, '合作', 'cooperating', 'crm_customer_status', '', 'success', 'N', 103, 1, '2026-06-07 00:00:00', '合作客户'),
(70011, '000000', 3, '暂停', 'paused', 'crm_customer_status', '', 'default', 'N', 103, 1, '2026-06-07 00:00:00', '暂停合作'),
(70012, '000000', 4, '黑名单', 'blacklist', 'crm_customer_status', '', 'danger', 'N', 103, 1, '2026-06-07 00:00:00', '黑名单客户'),
(70013, '000000', 1, '决策', 'decision', 'crm_decision_role', '', 'success', 'Y', 103, 1, '2026-06-07 00:00:00', '决策人'),
(70014, '000000', 2, '影响', 'influence', 'crm_decision_role', '', 'primary', 'N', 103, 1, '2026-06-07 00:00:00', '影响人'),
(70015, '000000', 3, '使用', 'user', 'crm_decision_role', '', 'info', 'N', 103, 1, '2026-06-07 00:00:00', '使用人'),
(70016, '000000', 4, '采购', 'buyer', 'crm_decision_role', '', 'warning', 'N', 103, 1, '2026-06-07 00:00:00', '采购人'),
(70017, '000000', 1, '市场', 'marketing', 'crm_opportunity_source', '', 'primary', 'Y', 103, 1, '2026-06-07 00:00:00', '市场来源'),
(70018, '000000', 2, '客户推荐', 'referral', 'crm_opportunity_source', '', 'success', 'N', 103, 1, '2026-06-07 00:00:00', '客户推荐'),
(70019, '000000', 3, '其他', 'other', 'crm_opportunity_source', '', 'default', 'N', 103, 1, '2026-06-07 00:00:00', '其他来源'),
(70020, '000000', 1, '线索', 'lead', 'crm_opportunity_stage', '', 'default', 'Y', 103, 1, '2026-06-07 00:00:00', '线索'),
(70021, '000000', 2, '需求沟通', 'requirement', 'crm_opportunity_stage', '', 'processing', 'N', 103, 1, '2026-06-07 00:00:00', '需求沟通'),
(70022, '000000', 3, '方案设计', 'solution', 'crm_opportunity_stage', '', 'processing', 'N', 103, 1, '2026-06-07 00:00:00', '方案设计'),
(70023, '000000', 4, '报价', 'quote', 'crm_opportunity_stage', '', 'warning', 'N', 103, 1, '2026-06-07 00:00:00', '报价'),
(70024, '000000', 5, '商务谈判', 'negotiation', 'crm_opportunity_stage', '', 'warning', 'N', 103, 1, '2026-06-07 00:00:00', '商务谈判'),
(70025, '000000', 6, '待签约', 'pending_contract', 'crm_opportunity_stage', '', 'primary', 'N', 103, 1, '2026-06-07 00:00:00', '待签约'),
(70026, '000000', 7, '已成交', 'won', 'crm_opportunity_stage', '', 'success', 'N', 103, 1, '2026-06-07 00:00:00', '已成交'),
(70027, '000000', 8, '失败', 'lost', 'crm_opportunity_stage', '', 'danger', 'N', 103, 1, '2026-06-07 00:00:00', '失败'),
(70028, '000000', 1, '电话', 'phone', 'crm_follow_method', '', 'primary', 'Y', 103, 1, '2026-06-07 00:00:00', '电话'),
(70029, '000000', 2, '微信', 'wechat', 'crm_follow_method', '', 'success', 'N', 103, 1, '2026-06-07 00:00:00', '微信'),
(70030, '000000', 3, '邮件', 'email', 'crm_follow_method', '', 'info', 'N', 103, 1, '2026-06-07 00:00:00', '邮件'),
(70031, '000000', 4, '现场', 'onsite', 'crm_follow_method', '', 'warning', 'N', 103, 1, '2026-06-07 00:00:00', '现场'),
(70032, '000000', 5, '视频', 'video', 'crm_follow_method', '', 'processing', 'N', 103, 1, '2026-06-07 00:00:00', '视频'),
(70033, '000000', 1, '继续推进', 'continue', 'crm_follow_result', '', 'success', 'Y', 103, 1, '2026-06-07 00:00:00', '继续推进'),
(70034, '000000', 2, '等待反馈', 'waiting', 'crm_follow_result', '', 'warning', 'N', 103, 1, '2026-06-07 00:00:00', '等待反馈'),
(70035, '000000', 3, '暂停', 'paused', 'crm_follow_result', '', 'default', 'N', 103, 1, '2026-06-07 00:00:00', '暂停'),
(70036, '000000', 4, '失败', 'failed', 'crm_follow_result', '', 'danger', 'N', 103, 1, '2026-06-07 00:00:00', '失败'),
(70037, '000000', 1, '草稿', 'draft', 'crm_quote_status', '', 'default', 'Y', 103, 1, '2026-06-07 00:00:00', '草稿'),
(70038, '000000', 2, '审批中', 'approving', 'crm_quote_status', '', 'processing', 'N', 103, 1, '2026-06-07 00:00:00', '审批中'),
(70039, '000000', 3, '已发送', 'sent', 'crm_quote_status', '', 'primary', 'N', 103, 1, '2026-06-07 00:00:00', '已发送'),
(70040, '000000', 4, '客户确认', 'confirmed', 'crm_quote_status', '', 'success', 'N', 103, 1, '2026-06-07 00:00:00', '客户确认'),
(70041, '000000', 5, '废弃', 'abandoned', 'crm_quote_status', '', 'danger', 'N', 103, 1, '2026-06-07 00:00:00', '废弃'),
(70042, '000000', 1, '草稿', 'draft', 'crm_contract_status', '', 'default', 'Y', 103, 1, '2026-06-07 00:00:00', '草稿'),
(70043, '000000', 2, '审批中', 'approving', 'crm_contract_status', '', 'processing', 'N', 103, 1, '2026-06-07 00:00:00', '审批中'),
(70044, '000000', 3, '待签署', 'pending_sign', 'crm_contract_status', '', 'warning', 'N', 103, 1, '2026-06-07 00:00:00', '待签署'),
(70045, '000000', 4, '执行中', 'executing', 'crm_contract_status', '', 'primary', 'N', 103, 1, '2026-06-07 00:00:00', '执行中'),
(70046, '000000', 5, '完成', 'completed', 'crm_contract_status', '', 'success', 'N', 103, 1, '2026-06-07 00:00:00', '完成'),
(70047, '000000', 6, '终止', 'terminated', 'crm_contract_status', '', 'danger', 'N', 103, 1, '2026-06-07 00:00:00', '终止'),
(70048, '000000', 1, '未到期', 'not_due', 'crm_payment_status', '', 'default', 'Y', 103, 1, '2026-06-07 00:00:00', '未到期'),
(70049, '000000', 2, '待收款', 'pending', 'crm_payment_status', '', 'warning', 'N', 103, 1, '2026-06-07 00:00:00', '待收款'),
(70050, '000000', 3, '已收款', 'received', 'crm_payment_status', '', 'success', 'N', 103, 1, '2026-06-07 00:00:00', '已收款'),
(70051, '000000', 4, '逾期', 'overdue', 'crm_payment_status', '', 'danger', 'N', 103, 1, '2026-06-07 00:00:00', '逾期')
ON CONFLICT ("dict_code") DO NOTHING;

INSERT INTO "sys_menu" ("menu_id", "menu_name", "parent_id", "order_num", "path", "component", "query_param", "is_frame", "is_cache", "menu_type", "visible", "status", "perms", "icon", "create_dept", "create_by", "create_time", "remark") VALUES
(7000, '客户中心', 0, 20, 'crm', '', '', 1, 0, 'M', '0', '0', '', 'mdi:account-briefcase-outline', 103, 1, '2026-06-07 00:00:00', 'XTP-CRM客户中心目录'),
(7001, '客户管理', 7000, 1, 'customer', 'crm/customer/index', '', 1, 0, 'C', '0', '0', 'crm:customer:list', 'mdi:office-building-outline', 103, 1, '2026-06-07 00:00:00', 'CRM客户管理菜单'),
(7002, '联系人管理', 7000, 2, 'contact', 'crm/contact/index', '', 1, 0, 'C', '0', '0', 'crm:contact:list', 'mdi:card-account-phone-outline', 103, 1, '2026-06-07 00:00:00', 'CRM联系人管理菜单'),
(7003, '商机管理', 7000, 3, 'opportunity', 'crm/opportunity/index', '', 1, 0, 'C', '0', '0', 'crm:opportunity:list', 'mdi:target-account', 103, 1, '2026-06-07 00:00:00', 'CRM商机管理菜单'),
(7004, '跟进记录', 7000, 4, 'follow-record', 'crm/follow-record/index', '', 1, 0, 'C', '0', '0', 'crm:followRecord:list', 'mdi:clipboard-text-clock-outline', 103, 1, '2026-06-07 00:00:00', 'CRM跟进记录菜单'),
(7005, '报价管理', 7000, 5, 'quote', 'crm/quote/index', '', 1, 0, 'C', '0', '0', 'crm:quote:list', 'mdi:file-document-edit-outline', 103, 1, '2026-06-07 00:00:00', 'CRM报价管理菜单'),
(7006, '合同管理', 7000, 6, 'contract', 'crm/contract/index', '', 1, 0, 'C', '0', '0', 'crm:contract:list', 'mdi:file-sign', 103, 1, '2026-06-07 00:00:00', 'CRM合同管理菜单'),
(7007, '回款计划', 7000, 7, 'payment-plan', 'crm/payment-plan/index', '', 1, 0, 'C', '0', '0', 'crm:paymentPlan:list', 'mdi:cash-clock', 103, 1, '2026-06-07 00:00:00', 'CRM回款计划菜单')
ON CONFLICT ("menu_id") DO NOTHING;

INSERT INTO "sys_menu" ("menu_id", "menu_name", "parent_id", "order_num", "path", "component", "query_param", "is_frame", "is_cache", "menu_type", "visible", "status", "perms", "icon", "create_dept", "create_by", "create_time", "remark") VALUES
(7011, '客户查询', 7001, 1, '', '', '', 1, 0, 'F', '0', '0', 'crm:customer:query', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7012, '客户新增', 7001, 2, '', '', '', 1, 0, 'F', '0', '0', 'crm:customer:add', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7013, '客户修改', 7001, 3, '', '', '', 1, 0, 'F', '0', '0', 'crm:customer:edit', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7014, '客户删除', 7001, 4, '', '', '', 1, 0, 'F', '0', '0', 'crm:customer:remove', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7015, '客户导出', 7001, 5, '', '', '', 1, 0, 'F', '0', '0', 'crm:customer:export', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7021, '联系人查询', 7002, 1, '', '', '', 1, 0, 'F', '0', '0', 'crm:contact:query', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7022, '联系人新增', 7002, 2, '', '', '', 1, 0, 'F', '0', '0', 'crm:contact:add', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7023, '联系人修改', 7002, 3, '', '', '', 1, 0, 'F', '0', '0', 'crm:contact:edit', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7024, '联系人删除', 7002, 4, '', '', '', 1, 0, 'F', '0', '0', 'crm:contact:remove', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7025, '联系人导出', 7002, 5, '', '', '', 1, 0, 'F', '0', '0', 'crm:contact:export', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7031, '商机查询', 7003, 1, '', '', '', 1, 0, 'F', '0', '0', 'crm:opportunity:query', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7032, '商机新增', 7003, 2, '', '', '', 1, 0, 'F', '0', '0', 'crm:opportunity:add', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7033, '商机修改', 7003, 3, '', '', '', 1, 0, 'F', '0', '0', 'crm:opportunity:edit', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7034, '商机删除', 7003, 4, '', '', '', 1, 0, 'F', '0', '0', 'crm:opportunity:remove', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7035, '商机导出', 7003, 5, '', '', '', 1, 0, 'F', '0', '0', 'crm:opportunity:export', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7041, '跟进查询', 7004, 1, '', '', '', 1, 0, 'F', '0', '0', 'crm:followRecord:query', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7042, '跟进新增', 7004, 2, '', '', '', 1, 0, 'F', '0', '0', 'crm:followRecord:add', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7043, '跟进修改', 7004, 3, '', '', '', 1, 0, 'F', '0', '0', 'crm:followRecord:edit', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7044, '跟进删除', 7004, 4, '', '', '', 1, 0, 'F', '0', '0', 'crm:followRecord:remove', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7045, '跟进导出', 7004, 5, '', '', '', 1, 0, 'F', '0', '0', 'crm:followRecord:export', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7051, '报价查询', 7005, 1, '', '', '', 1, 0, 'F', '0', '0', 'crm:quote:query', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7052, '报价新增', 7005, 2, '', '', '', 1, 0, 'F', '0', '0', 'crm:quote:add', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7053, '报价修改', 7005, 3, '', '', '', 1, 0, 'F', '0', '0', 'crm:quote:edit', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7054, '报价删除', 7005, 4, '', '', '', 1, 0, 'F', '0', '0', 'crm:quote:remove', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7055, '报价导出', 7005, 5, '', '', '', 1, 0, 'F', '0', '0', 'crm:quote:export', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7061, '合同查询', 7006, 1, '', '', '', 1, 0, 'F', '0', '0', 'crm:contract:query', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7062, '合同新增', 7006, 2, '', '', '', 1, 0, 'F', '0', '0', 'crm:contract:add', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7063, '合同修改', 7006, 3, '', '', '', 1, 0, 'F', '0', '0', 'crm:contract:edit', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7064, '合同删除', 7006, 4, '', '', '', 1, 0, 'F', '0', '0', 'crm:contract:remove', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7065, '合同导出', 7006, 5, '', '', '', 1, 0, 'F', '0', '0', 'crm:contract:export', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7071, '回款查询', 7007, 1, '', '', '', 1, 0, 'F', '0', '0', 'crm:paymentPlan:query', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7072, '回款新增', 7007, 2, '', '', '', 1, 0, 'F', '0', '0', 'crm:paymentPlan:add', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7073, '回款修改', 7007, 3, '', '', '', 1, 0, 'F', '0', '0', 'crm:paymentPlan:edit', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7074, '回款删除', 7007, 4, '', '', '', 1, 0, 'F', '0', '0', 'crm:paymentPlan:remove', '#', 103, 1, '2026-06-07 00:00:00', ''),
(7075, '回款导出', 7007, 5, '', '', '', 1, 0, 'F', '0', '0', 'crm:paymentPlan:export', '#', 103, 1, '2026-06-07 00:00:00', '')
ON CONFLICT ("menu_id") DO NOTHING;
