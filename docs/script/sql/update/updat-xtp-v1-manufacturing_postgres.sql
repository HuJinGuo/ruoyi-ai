-- XTP V1 合同驱动制造闭环表结构、字典与菜单
-- 执行环境：PostgreSQL


CREATE TABLE IF NOT EXISTS "material_part" (
  "part_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "part_code" varchar(64) NOT NULL,
  "part_name" varchar(120) NOT NULL,
  "specification" varchar(255),
  "unit" varchar(32),
  "material" varchar(80),
  "category" varchar(32),
  "default_supplier_id" bigint,
  "status" varchar(32) DEFAULT 'enabled',
  "remark" varchar(500),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("part_id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_material_part_tenant_code" ON "material_part" ("tenant_id", "part_code");
CREATE INDEX IF NOT EXISTS "idx_material_part_name" ON "material_part" ("part_name");

CREATE TABLE IF NOT EXISTS "srm_supplier" (
  "supplier_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "supplier_code" varchar(64) NOT NULL,
  "supplier_name" varchar(120) NOT NULL,
  "short_name" varchar(80),
  "contact_name" varchar(80),
  "phone" varchar(32),
  "email" varchar(120),
  "address" varchar(255),
  "level" varchar(16),
  "status" varchar(32) DEFAULT 'active',
  "remark" text,
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("supplier_id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_srm_supplier_tenant_code" ON "srm_supplier" ("tenant_id", "supplier_code");
CREATE INDEX IF NOT EXISTS "idx_srm_supplier_name" ON "srm_supplier" ("supplier_name");

CREATE TABLE IF NOT EXISTS "mes_work_order" (
  "work_order_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "contract_id" bigint NOT NULL,
  "customer_id" bigint,
  "opportunity_id" bigint,
  "work_order_code" varchar(64) NOT NULL,
  "project_name" varchar(120) NOT NULL,
  "product_name" varchar(120),
  "quantity" int DEFAULT 1,
  "current_stage" varchar(32),
  "progress" numeric(10,2) DEFAULT 0,
  "status" varchar(32) DEFAULT 'pending',
  "plan_delivery_date" date,
  "actual_delivery_date" date,
  "responsible_user_id" bigint,
  "remark" varchar(500),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("work_order_id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_mes_work_order_tenant_code" ON "mes_work_order" ("tenant_id", "work_order_code");
CREATE INDEX IF NOT EXISTS "idx_mes_work_order_contract" ON "mes_work_order" ("contract_id");
CREATE INDEX IF NOT EXISTS "idx_mes_work_order_status" ON "mes_work_order" ("status");

CREATE TABLE IF NOT EXISTS "engineering_material" (
  "engineering_material_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "work_order_id" bigint NOT NULL,
  "contract_id" bigint,
  "part_id" bigint,
  "part_code" varchar(64),
  "part_name" varchar(120),
  "specification" varchar(255),
  "unit" varchar(32),
  "required_qty" numeric(18,4) DEFAULT 0,
  "stock_qty" numeric(18,4) DEFAULT 0,
  "shortage_qty" numeric(18,4) DEFAULT 0,
  "purchase_qty" numeric(18,4) DEFAULT 0,
  "status" varchar(32) DEFAULT 'pending_check',
  "remark" varchar(500),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("engineering_material_id")
);
CREATE INDEX IF NOT EXISTS "idx_engineering_material_work_order" ON "engineering_material" ("work_order_id");
CREATE INDEX IF NOT EXISTS "idx_engineering_material_part" ON "engineering_material" ("part_id");
CREATE INDEX IF NOT EXISTS "idx_engineering_material_status" ON "engineering_material" ("status");

CREATE TABLE IF NOT EXISTS "wms_inventory" (
  "inventory_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "part_id" bigint NOT NULL,
  "part_code" varchar(64),
  "part_name" varchar(120),
  "specification" varchar(255),
  "unit" varchar(32),
  "stock_qty" numeric(18,4) DEFAULT 0,
  "available_qty" numeric(18,4) DEFAULT 0,
  "locked_qty" numeric(18,4) DEFAULT 0,
  "location_code" varchar(64),
  "remark" varchar(500),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("inventory_id")
);
CREATE INDEX IF NOT EXISTS "idx_wms_inventory_part" ON "wms_inventory" ("part_id");
CREATE INDEX IF NOT EXISTS "idx_wms_inventory_location" ON "wms_inventory" ("location_code");

CREATE TABLE IF NOT EXISTS "srm_purchase_request" (
  "purchase_request_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "work_order_id" bigint NOT NULL,
  "contract_id" bigint,
  "engineering_material_id" bigint,
  "supplier_id" bigint,
  "part_id" bigint,
  "part_code" varchar(64),
  "part_name" varchar(120),
  "specification" varchar(255),
  "unit" varchar(32),
  "request_qty" numeric(18,4) DEFAULT 0,
  "status" varchar(32) DEFAULT 'pending',
  "remark" varchar(500),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("purchase_request_id")
);
CREATE INDEX IF NOT EXISTS "idx_srm_pr_work_order" ON "srm_purchase_request" ("work_order_id");
CREATE INDEX IF NOT EXISTS "idx_srm_pr_material" ON "srm_purchase_request" ("engineering_material_id");
CREATE INDEX IF NOT EXISTS "idx_srm_pr_status" ON "srm_purchase_request" ("status");

CREATE TABLE IF NOT EXISTS "srm_purchase_order" (
  "purchase_order_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "purchase_order_code" varchar(64) NOT NULL,
  "purchase_request_id" bigint,
  "supplier_id" bigint,
  "work_order_id" bigint,
  "contract_id" bigint,
  "status" varchar(32) DEFAULT 'draft',
  "order_date" date,
  "expected_delivery_date" date,
  "remark" varchar(500),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("purchase_order_id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "uk_srm_po_tenant_code" ON "srm_purchase_order" ("tenant_id", "purchase_order_code");
CREATE INDEX IF NOT EXISTS "idx_srm_po_request" ON "srm_purchase_order" ("purchase_request_id");
CREATE INDEX IF NOT EXISTS "idx_srm_po_work_order" ON "srm_purchase_order" ("work_order_id");
CREATE INDEX IF NOT EXISTS "idx_srm_po_status" ON "srm_purchase_order" ("status");

CREATE TABLE IF NOT EXISTS "srm_purchase_order_item" (
  "purchase_order_item_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "purchase_order_id" bigint NOT NULL,
  "purchase_request_id" bigint,
  "work_order_id" bigint,
  "contract_id" bigint,
  "part_id" bigint,
  "part_code" varchar(64),
  "part_name" varchar(120),
  "specification" varchar(255),
  "unit" varchar(32),
  "purchase_qty" numeric(18,4) DEFAULT 0,
  "price" numeric(18,4) DEFAULT 0,
  "amount" numeric(18,2) DEFAULT 0,
  "received_qty" numeric(18,4) DEFAULT 0,
  "status" varchar(32) DEFAULT 'purchasing',
  "remark" varchar(500),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("purchase_order_item_id")
);
CREATE INDEX IF NOT EXISTS "idx_srm_poi_order" ON "srm_purchase_order_item" ("purchase_order_id");
CREATE INDEX IF NOT EXISTS "idx_srm_poi_work_order" ON "srm_purchase_order_item" ("work_order_id");
CREATE INDEX IF NOT EXISTS "idx_srm_poi_status" ON "srm_purchase_order_item" ("status");

CREATE TABLE IF NOT EXISTS "wms_receipt_order" (
  "receipt_order_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "purchase_order_id" bigint NOT NULL,
  "supplier_id" bigint,
  "work_order_id" bigint,
  "contract_id" bigint,
  "receipt_status" varchar(32) DEFAULT 'pending',
  "receipt_time" timestamp,
  "warehouse_user_id" bigint,
  "remark" varchar(500),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("receipt_order_id")
);
CREATE INDEX IF NOT EXISTS "idx_wms_receipt_po" ON "wms_receipt_order" ("purchase_order_id");
CREATE INDEX IF NOT EXISTS "idx_wms_receipt_work_order" ON "wms_receipt_order" ("work_order_id");
CREATE INDEX IF NOT EXISTS "idx_wms_receipt_status" ON "wms_receipt_order" ("receipt_status");

CREATE TABLE IF NOT EXISTS "wms_receipt_order_item" (
  "receipt_order_item_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "receipt_order_id" bigint NOT NULL,
  "purchase_order_id" bigint,
  "purchase_order_item_id" bigint,
  "work_order_id" bigint,
  "contract_id" bigint,
  "part_id" bigint,
  "part_code" varchar(64),
  "part_name" varchar(120),
  "specification" varchar(255),
  "unit" varchar(32),
  "receipt_qty" numeric(18,4) DEFAULT 0,
  "status" varchar(32) DEFAULT 'pending',
  "remark" varchar(500),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("receipt_order_item_id")
);
CREATE INDEX IF NOT EXISTS "idx_wms_receipt_item_order" ON "wms_receipt_order_item" ("receipt_order_id");
CREATE INDEX IF NOT EXISTS "idx_wms_receipt_item_po" ON "wms_receipt_order_item" ("purchase_order_id");

CREATE TABLE IF NOT EXISTS "wms_issue_order" (
  "issue_order_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "work_order_id" bigint NOT NULL,
  "contract_id" bigint,
  "issue_status" varchar(32) DEFAULT 'pending',
  "issue_time" timestamp,
  "warehouse_user_id" bigint,
  "remark" varchar(500),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("issue_order_id")
);
CREATE INDEX IF NOT EXISTS "idx_wms_issue_work_order" ON "wms_issue_order" ("work_order_id");
CREATE INDEX IF NOT EXISTS "idx_wms_issue_status" ON "wms_issue_order" ("issue_status");

CREATE TABLE IF NOT EXISTS "wms_issue_order_item" (
  "issue_order_item_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "issue_order_id" bigint NOT NULL,
  "work_order_id" bigint,
  "contract_id" bigint,
  "engineering_material_id" bigint,
  "part_id" bigint,
  "part_code" varchar(64),
  "part_name" varchar(120),
  "specification" varchar(255),
  "unit" varchar(32),
  "issue_qty" numeric(18,4) DEFAULT 0,
  "status" varchar(32) DEFAULT 'pending',
  "remark" varchar(500),
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("issue_order_item_id")
);
CREATE INDEX IF NOT EXISTS "idx_wms_issue_item_order" ON "wms_issue_order_item" ("issue_order_id");
CREATE INDEX IF NOT EXISTS "idx_wms_issue_item_work_order" ON "wms_issue_order_item" ("work_order_id");

CREATE TABLE IF NOT EXISTS "mes_work_order_stage" (
  "work_order_stage_id" bigint NOT NULL,
  "tenant_id" varchar(20) DEFAULT '000000',
  "work_order_id" bigint NOT NULL,
  "stage_code" varchar(32) NOT NULL,
  "stage_name" varchar(64) NOT NULL,
  "status" varchar(32) DEFAULT 'WAIT',
  "responsible_user_id" bigint,
  "start_time" timestamp,
  "end_time" timestamp,
  "remark" text,
  "create_dept" bigint,
  "create_by" bigint,
  "create_time" timestamp,
  "update_by" bigint,
  "update_time" timestamp,
  PRIMARY KEY ("work_order_stage_id")
);
CREATE INDEX IF NOT EXISTS "idx_mes_stage_work_order" ON "mes_work_order_stage" ("work_order_id");
CREATE INDEX IF NOT EXISTS "idx_mes_stage_code" ON "mes_work_order_stage" ("stage_code");
CREATE INDEX IF NOT EXISTS "idx_mes_stage_status" ON "mes_work_order_stage" ("status");

INSERT INTO "sys_dict_type" ("dict_id", "tenant_id", "dict_name", "dict_type", "create_dept", "create_by", "create_time", "remark") VALUES
(7101, '000000', 'XTP物料分类', 'xtp_material_category', 103, 1, '2026-06-08 00:00:00', 'XTP物料分类'),
(7102, '000000', 'XTP启停状态', 'xtp_enable_status', 103, 1, '2026-06-08 00:00:00', 'XTP启停状态'),
(7103, '000000', 'XTP供应商等级', 'xtp_supplier_level', 103, 1, '2026-06-08 00:00:00', 'XTP供应商等级'),
(7104, '000000', 'XTP供应商状态', 'xtp_supplier_status', 103, 1, '2026-06-08 00:00:00', 'XTP供应商状态'),
(7105, '000000', 'XTP工单状态', 'xtp_work_order_status', 103, 1, '2026-06-08 00:00:00', 'XTP工单状态'),
(7106, '000000', 'XTP阶段状态', 'xtp_stage_status', 103, 1, '2026-06-08 00:00:00', 'XTP阶段状态'),
(7107, '000000', 'XTP工程物料状态', 'xtp_engineering_material_status', 103, 1, '2026-06-08 00:00:00', 'XTP工程物料状态'),
(7108, '000000', 'XTP采购需求状态', 'xtp_purchase_request_status', 103, 1, '2026-06-08 00:00:00', 'XTP采购需求状态'),
(7109, '000000', 'XTP采购订单状态', 'xtp_purchase_order_status', 103, 1, '2026-06-08 00:00:00', 'XTP采购订单状态'),
(7110, '000000', 'XTP收料状态', 'xtp_receipt_status', 103, 1, '2026-06-08 00:00:00', 'XTP收料状态'),
(7111, '000000', 'XTP发料状态', 'xtp_issue_status', 103, 1, '2026-06-08 00:00:00', 'XTP发料状态')
ON CONFLICT ("dict_id") DO NOTHING;

INSERT INTO "sys_dict_data" ("dict_code", "tenant_id", "dict_sort", "dict_label", "dict_value", "dict_type", "css_class", "list_class", "is_default", "create_dept", "create_by", "create_time", "remark") VALUES
(71001, '000000', 1, '机加件', 'machined', 'xtp_material_category', '', 'default', 'N', 103, 1, '2026-06-08 00:00:00', '机加件'),
(71002, '000000', 2, '钣金件', 'sheet_metal', 'xtp_material_category', '', 'processing', 'N', 103, 1, '2026-06-08 00:00:00', '钣金件'),
(71003, '000000', 3, '标准件', 'standard', 'xtp_material_category', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', '标准件'),
(71004, '000000', 4, '电气件', 'electrical', 'xtp_material_category', '', 'warning', 'N', 103, 1, '2026-06-08 00:00:00', '电气件'),
(71005, '000000', 5, '外购件', 'purchased', 'xtp_material_category', '', 'primary', 'N', 103, 1, '2026-06-08 00:00:00', '外购件'),
(71006, '000000', 6, '其他', 'other', 'xtp_material_category', '', 'info', 'N', 103, 1, '2026-06-08 00:00:00', '其他'),
(71007, '000000', 1, '启用', 'enabled', 'xtp_enable_status', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', '启用'),
(71008, '000000', 2, '停用', 'disabled', 'xtp_enable_status', '', 'danger', 'N', 103, 1, '2026-06-08 00:00:00', '停用'),
(71009, '000000', 1, 'A', 'a', 'xtp_supplier_level', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', 'A'),
(71010, '000000', 2, 'B', 'b', 'xtp_supplier_level', '', 'primary', 'N', 103, 1, '2026-06-08 00:00:00', 'B'),
(71011, '000000', 3, 'C', 'c', 'xtp_supplier_level', '', 'warning', 'N', 103, 1, '2026-06-08 00:00:00', 'C'),
(71012, '000000', 4, 'D', 'd', 'xtp_supplier_level', '', 'danger', 'N', 103, 1, '2026-06-08 00:00:00', 'D'),
(71013, '000000', 1, '合作中', 'active', 'xtp_supplier_status', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', '合作中'),
(71014, '000000', 2, '暂停合作', 'suspended', 'xtp_supplier_status', '', 'warning', 'N', 103, 1, '2026-06-08 00:00:00', '暂停合作'),
(71015, '000000', 3, '黑名单', 'blacklist', 'xtp_supplier_status', '', 'danger', 'N', 103, 1, '2026-06-08 00:00:00', '黑名单'),
(71016, '000000', 1, '待生产', 'pending', 'xtp_work_order_status', '', 'default', 'N', 103, 1, '2026-06-08 00:00:00', '待生产'),
(71017, '000000', 2, '进行中', 'processing', 'xtp_work_order_status', '', 'processing', 'N', 103, 1, '2026-06-08 00:00:00', '进行中'),
(71018, '000000', 3, '暂停', 'paused', 'xtp_work_order_status', '', 'warning', 'N', 103, 1, '2026-06-08 00:00:00', '暂停'),
(71019, '000000', 4, '完成', 'completed', 'xtp_work_order_status', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', '完成'),
(71020, '000000', 5, '取消', 'canceled', 'xtp_work_order_status', '', 'danger', 'N', 103, 1, '2026-06-08 00:00:00', '取消'),
(71021, '000000', 1, '等待', 'WAIT', 'xtp_stage_status', '', 'default', 'N', 103, 1, '2026-06-08 00:00:00', '等待'),
(71022, '000000', 2, '进行中', 'PROCESSING', 'xtp_stage_status', '', 'processing', 'N', 103, 1, '2026-06-08 00:00:00', '进行中'),
(71023, '000000', 3, '完成', 'FINISHED', 'xtp_stage_status', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', '完成'),
(71024, '000000', 4, '暂停', 'PAUSE', 'xtp_stage_status', '', 'warning', 'N', 103, 1, '2026-06-08 00:00:00', '暂停'),
(71025, '000000', 1, '待检查', 'pending_check', 'xtp_engineering_material_status', '', 'default', 'N', 103, 1, '2026-06-08 00:00:00', '待检查'),
(71026, '000000', 2, '库存充足', 'stock_enough', 'xtp_engineering_material_status', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', '库存充足'),
(71027, '000000', 3, '部分缺料', 'partial_shortage', 'xtp_engineering_material_status', '', 'warning', 'N', 103, 1, '2026-06-08 00:00:00', '部分缺料'),
(71028, '000000', 4, '全部缺料', 'full_shortage', 'xtp_engineering_material_status', '', 'danger', 'N', 103, 1, '2026-06-08 00:00:00', '全部缺料'),
(71029, '000000', 5, '已采购', 'purchased', 'xtp_engineering_material_status', '', 'processing', 'N', 103, 1, '2026-06-08 00:00:00', '已采购'),
(71030, '000000', 6, '已收料', 'received', 'xtp_engineering_material_status', '', 'primary', 'N', 103, 1, '2026-06-08 00:00:00', '已收料'),
(71031, '000000', 7, '已发料', 'issued', 'xtp_engineering_material_status', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', '已发料'),
(71032, '000000', 1, '待采购', 'pending', 'xtp_purchase_request_status', '', 'default', 'N', 103, 1, '2026-06-08 00:00:00', '待采购'),
(71033, '000000', 2, '采购中', 'purchasing', 'xtp_purchase_request_status', '', 'processing', 'N', 103, 1, '2026-06-08 00:00:00', '采购中'),
(71034, '000000', 3, '已完成', 'completed', 'xtp_purchase_request_status', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', '已完成'),
(71035, '000000', 4, '取消', 'canceled', 'xtp_purchase_request_status', '', 'danger', 'N', 103, 1, '2026-06-08 00:00:00', '取消'),
(71036, '000000', 1, '草稿', 'draft', 'xtp_purchase_order_status', '', 'default', 'N', 103, 1, '2026-06-08 00:00:00', '草稿'),
(71037, '000000', 2, '已下单', 'ordered', 'xtp_purchase_order_status', '', 'processing', 'N', 103, 1, '2026-06-08 00:00:00', '已下单'),
(71038, '000000', 3, '部分到货', 'partial_arrived', 'xtp_purchase_order_status', '', 'warning', 'N', 103, 1, '2026-06-08 00:00:00', '部分到货'),
(71039, '000000', 4, '全部到货', 'arrived_all', 'xtp_purchase_order_status', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', '全部到货'),
(71040, '000000', 5, '关闭', 'closed', 'xtp_purchase_order_status', '', 'danger', 'N', 103, 1, '2026-06-08 00:00:00', '关闭'),
(71041, '000000', 1, '待收料', 'pending', 'xtp_receipt_status', '', 'default', 'N', 103, 1, '2026-06-08 00:00:00', '待收料'),
(71042, '000000', 2, '部分收料', 'partial', 'xtp_receipt_status', '', 'warning', 'N', 103, 1, '2026-06-08 00:00:00', '部分收料'),
(71043, '000000', 3, '收料完成', 'completed', 'xtp_receipt_status', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', '收料完成'),
(71044, '000000', 1, '待发料', 'pending', 'xtp_issue_status', '', 'default', 'N', 103, 1, '2026-06-08 00:00:00', '待发料'),
(71045, '000000', 2, '部分发料', 'partial', 'xtp_issue_status', '', 'warning', 'N', 103, 1, '2026-06-08 00:00:00', '部分发料'),
(71046, '000000', 3, '发料完成', 'completed', 'xtp_issue_status', '', 'success', 'N', 103, 1, '2026-06-08 00:00:00', '发料完成')
ON CONFLICT ("dict_code") DO NOTHING;

INSERT INTO "sys_menu" ("menu_id", "menu_name", "parent_id", "order_num", "path", "component", "query_param", "is_frame", "is_cache", "menu_type", "visible", "status", "perms", "icon", "create_dept", "create_by", "create_time", "remark") VALUES
(7100, '物料中心', 0, 21, 'material', '', '', 1, 0, 'M', '0', '0', '', 'mdi:package-variant-closed', 103, 1, '2026-06-08 00:00:00', 'XTP物料中心'),
(7200, '工程中心', 0, 22, 'engineering', '', '', 1, 0, 'M', '0', '0', '', 'mdi:clipboard-list-outline', 103, 1, '2026-06-08 00:00:00', 'XTP工程中心'),
(7300, '采购中心', 0, 23, 'srm', '', '', 1, 0, 'M', '0', '0', '', 'mdi:cart-outline', 103, 1, '2026-06-08 00:00:00', 'XTP采购中心'),
(7400, '仓库中心', 0, 24, 'wms', '', '', 1, 0, 'M', '0', '0', '', 'mdi:warehouse', 103, 1, '2026-06-08 00:00:00', 'XTP仓库中心'),
(7500, '生产中心', 0, 25, 'mes', '', '', 1, 0, 'M', '0', '0', '', 'mdi:factory', 103, 1, '2026-06-08 00:00:00', 'XTP生产中心'),
(7101, '物料管理', 7100, 1, 'part', 'material/part/index', '', 1, 0, 'C', '0', '0', 'material:part:list', 'mdi:cube-outline', 103, 1, '2026-06-08 00:00:00', 'XTP物料管理'),
(7201, '工程清算', 7200, 1, 'material', 'engineering/material/index', '', 1, 0, 'C', '0', '0', 'engineering:material:list', 'mdi:clipboard-check-outline', 103, 1, '2026-06-08 00:00:00', 'XTP工程清算'),
(7301, '供应商管理', 7300, 1, 'supplier', 'srm/supplier/index', '', 1, 0, 'C', '0', '0', 'srm:supplier:list', 'mdi:account-hard-hat-outline', 103, 1, '2026-06-08 00:00:00', 'XTP供应商管理'),
(7302, '采购需求', 7300, 2, 'purchase-request', 'srm/purchase-request/index', '', 1, 0, 'C', '0', '0', 'srm:purchaseRequest:list', 'mdi:cart-arrow-down', 103, 1, '2026-06-08 00:00:00', 'XTP采购需求'),
(7303, '采购订单', 7300, 3, 'purchase-order', 'srm/purchase-order/index', '', 1, 0, 'C', '0', '0', 'srm:purchaseOrder:list', 'mdi:file-document-outline', 103, 1, '2026-06-08 00:00:00', 'XTP采购订单'),
(7304, '采购明细', 7300, 4, 'purchase-order-item', 'srm/purchase-order-item/index', '', 1, 0, 'C', '0', '0', 'srm:purchaseOrderItem:list', 'mdi:format-list-bulleted', 103, 1, '2026-06-08 00:00:00', 'XTP采购明细'),
(7401, '库存管理', 7400, 1, 'inventory', 'wms/inventory/index', '', 1, 0, 'C', '0', '0', 'wms:inventory:list', 'mdi:archive-search-outline', 103, 1, '2026-06-08 00:00:00', 'XTP库存管理'),
(7402, '收料单', 7400, 2, 'receipt-order', 'wms/receipt-order/index', '', 1, 0, 'C', '0', '0', 'wms:receiptOrder:list', 'mdi:tray-arrow-down', 103, 1, '2026-06-08 00:00:00', 'XTP收料单'),
(7403, '收料明细', 7400, 3, 'receipt-order-item', 'wms/receipt-order-item/index', '', 1, 0, 'C', '0', '0', 'wms:receiptOrderItem:list', 'mdi:format-list-checks', 103, 1, '2026-06-08 00:00:00', 'XTP收料明细'),
(7404, '发料单', 7400, 4, 'issue-order', 'wms/issue-order/index', '', 1, 0, 'C', '0', '0', 'wms:issueOrder:list', 'mdi:tray-arrow-up', 103, 1, '2026-06-08 00:00:00', 'XTP发料单'),
(7405, '发料明细', 7400, 5, 'issue-order-item', 'wms/issue-order-item/index', '', 1, 0, 'C', '0', '0', 'wms:issueOrderItem:list', 'mdi:playlist-check', 103, 1, '2026-06-08 00:00:00', 'XTP发料明细'),
(7501, '生产工单', 7500, 1, 'work-order', 'mes/work-order/index', '', 1, 0, 'C', '0', '0', 'mes:workOrder:list', 'mdi:clipboard-text-outline', 103, 1, '2026-06-08 00:00:00', 'XTP生产工单'),
(7502, '工单阶段', 7500, 2, 'work-order-stage', 'mes/work-order-stage/index', '', 1, 0, 'C', '0', '0', 'mes:workOrderStage:list', 'mdi:timeline-check-outline', 103, 1, '2026-06-08 00:00:00', 'XTP工单阶段'),
(7503, '项目看板', 7500, 3, 'work-order-board', 'mes/work-order-board/index', '', 1, 0, 'C', '0', '0', 'mes:workOrder:query', 'mdi:view-dashboard-outline', 103, 1, '2026-06-08 00:00:00', 'XTP项目看板')
ON CONFLICT ("menu_id") DO NOTHING;

INSERT INTO "sys_menu" ("menu_id", "menu_name", "parent_id", "order_num", "path", "component", "query_param", "is_frame", "is_cache", "menu_type", "visible", "status", "perms", "icon", "create_dept", "create_by", "create_time", "remark") VALUES
(71011, '物料管理查询', 7101, 1, '', '', '', 1, 0, 'F', '0', '0', 'material:part:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(71012, '物料管理新增', 7101, 2, '', '', '', 1, 0, 'F', '0', '0', 'material:part:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(71013, '物料管理修改', 7101, 3, '', '', '', 1, 0, 'F', '0', '0', 'material:part:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(71014, '物料管理删除', 7101, 4, '', '', '', 1, 0, 'F', '0', '0', 'material:part:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(71015, '物料管理导出', 7101, 5, '', '', '', 1, 0, 'F', '0', '0', 'material:part:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(72011, '工程清算查询', 7201, 1, '', '', '', 1, 0, 'F', '0', '0', 'engineering:material:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(72012, '工程清算新增', 7201, 2, '', '', '', 1, 0, 'F', '0', '0', 'engineering:material:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(72013, '工程清算修改', 7201, 3, '', '', '', 1, 0, 'F', '0', '0', 'engineering:material:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(72014, '工程清算删除', 7201, 4, '', '', '', 1, 0, 'F', '0', '0', 'engineering:material:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(72015, '工程清算导出', 7201, 5, '', '', '', 1, 0, 'F', '0', '0', 'engineering:material:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73011, '供应商管理查询', 7301, 1, '', '', '', 1, 0, 'F', '0', '0', 'srm:supplier:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73012, '供应商管理新增', 7301, 2, '', '', '', 1, 0, 'F', '0', '0', 'srm:supplier:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73013, '供应商管理修改', 7301, 3, '', '', '', 1, 0, 'F', '0', '0', 'srm:supplier:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73014, '供应商管理删除', 7301, 4, '', '', '', 1, 0, 'F', '0', '0', 'srm:supplier:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73015, '供应商管理导出', 7301, 5, '', '', '', 1, 0, 'F', '0', '0', 'srm:supplier:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73021, '采购需求查询', 7302, 1, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseRequest:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73022, '采购需求新增', 7302, 2, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseRequest:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73023, '采购需求修改', 7302, 3, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseRequest:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73024, '采购需求删除', 7302, 4, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseRequest:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73025, '采购需求导出', 7302, 5, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseRequest:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73031, '采购订单查询', 7303, 1, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseOrder:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73032, '采购订单新增', 7303, 2, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseOrder:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73033, '采购订单修改', 7303, 3, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseOrder:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73034, '采购订单删除', 7303, 4, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseOrder:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73035, '采购订单导出', 7303, 5, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseOrder:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73041, '采购明细查询', 7304, 1, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseOrderItem:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73042, '采购明细新增', 7304, 2, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseOrderItem:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73043, '采购明细修改', 7304, 3, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseOrderItem:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73044, '采购明细删除', 7304, 4, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseOrderItem:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(73045, '采购明细导出', 7304, 5, '', '', '', 1, 0, 'F', '0', '0', 'srm:purchaseOrderItem:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74011, '库存管理查询', 7401, 1, '', '', '', 1, 0, 'F', '0', '0', 'wms:inventory:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74012, '库存管理新增', 7401, 2, '', '', '', 1, 0, 'F', '0', '0', 'wms:inventory:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74013, '库存管理修改', 7401, 3, '', '', '', 1, 0, 'F', '0', '0', 'wms:inventory:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74014, '库存管理删除', 7401, 4, '', '', '', 1, 0, 'F', '0', '0', 'wms:inventory:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74015, '库存管理导出', 7401, 5, '', '', '', 1, 0, 'F', '0', '0', 'wms:inventory:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74021, '收料单查询', 7402, 1, '', '', '', 1, 0, 'F', '0', '0', 'wms:receiptOrder:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74022, '收料单新增', 7402, 2, '', '', '', 1, 0, 'F', '0', '0', 'wms:receiptOrder:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74023, '收料单修改', 7402, 3, '', '', '', 1, 0, 'F', '0', '0', 'wms:receiptOrder:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74024, '收料单删除', 7402, 4, '', '', '', 1, 0, 'F', '0', '0', 'wms:receiptOrder:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74025, '收料单导出', 7402, 5, '', '', '', 1, 0, 'F', '0', '0', 'wms:receiptOrder:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74031, '收料明细查询', 7403, 1, '', '', '', 1, 0, 'F', '0', '0', 'wms:receiptOrderItem:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74032, '收料明细新增', 7403, 2, '', '', '', 1, 0, 'F', '0', '0', 'wms:receiptOrderItem:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74033, '收料明细修改', 7403, 3, '', '', '', 1, 0, 'F', '0', '0', 'wms:receiptOrderItem:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74034, '收料明细删除', 7403, 4, '', '', '', 1, 0, 'F', '0', '0', 'wms:receiptOrderItem:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74035, '收料明细导出', 7403, 5, '', '', '', 1, 0, 'F', '0', '0', 'wms:receiptOrderItem:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74041, '发料单查询', 7404, 1, '', '', '', 1, 0, 'F', '0', '0', 'wms:issueOrder:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74042, '发料单新增', 7404, 2, '', '', '', 1, 0, 'F', '0', '0', 'wms:issueOrder:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74043, '发料单修改', 7404, 3, '', '', '', 1, 0, 'F', '0', '0', 'wms:issueOrder:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74044, '发料单删除', 7404, 4, '', '', '', 1, 0, 'F', '0', '0', 'wms:issueOrder:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74045, '发料单导出', 7404, 5, '', '', '', 1, 0, 'F', '0', '0', 'wms:issueOrder:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74051, '发料明细查询', 7405, 1, '', '', '', 1, 0, 'F', '0', '0', 'wms:issueOrderItem:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74052, '发料明细新增', 7405, 2, '', '', '', 1, 0, 'F', '0', '0', 'wms:issueOrderItem:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74053, '发料明细修改', 7405, 3, '', '', '', 1, 0, 'F', '0', '0', 'wms:issueOrderItem:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74054, '发料明细删除', 7405, 4, '', '', '', 1, 0, 'F', '0', '0', 'wms:issueOrderItem:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(74055, '发料明细导出', 7405, 5, '', '', '', 1, 0, 'F', '0', '0', 'wms:issueOrderItem:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75011, '生产工单查询', 7501, 1, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrder:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75012, '生产工单新增', 7501, 2, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrder:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75013, '生产工单修改', 7501, 3, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrder:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75014, '生产工单删除', 7501, 4, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrder:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75015, '生产工单导出', 7501, 5, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrder:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75021, '工单阶段查询', 7502, 1, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrderStage:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75022, '工单阶段新增', 7502, 2, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrderStage:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75023, '工单阶段修改', 7502, 3, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrderStage:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75024, '工单阶段删除', 7502, 4, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrderStage:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75025, '工单阶段导出', 7502, 5, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrderStage:export', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75031, '项目看板查询', 7503, 1, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrder:query', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75032, '项目看板新增', 7503, 2, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrder:add', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75033, '项目看板修改', 7503, 3, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrder:edit', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75034, '项目看板删除', 7503, 4, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrder:remove', '#', 103, 1, '2026-06-08 00:00:00', ''),
(75035, '项目看板导出', 7503, 5, '', '', '', 1, 0, 'F', '0', '0', 'mes:workOrder:export', '#', 103, 1, '2026-06-08 00:00:00', '')
ON CONFLICT ("menu_id") DO NOTHING;
