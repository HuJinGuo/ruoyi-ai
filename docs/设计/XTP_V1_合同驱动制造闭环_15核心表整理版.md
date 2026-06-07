# XTP V1 合同驱动制造闭环设计文档

> 当前版本目标：先跑通 **合同 → 工单 → 工程清算 → 库存检查 → 采购 → 收料 → 发料 → 生产阶段推进 → 发货/售后/验收** 的主流程。  
> 当前不是完整 ERP，也不是完整 MES/PDM/SRM/WMS，而是先做一个可以演示、可以扩展、可以接 AI 的 V1 流程闭环。

---

# 一、V1 核心业务流程

```text
CRM合同
  ↓
生成生产工单
  ↓
工程物料清算
  ↓
WMS库存检查
  ↓
缺料生成采购需求
  ↓
采购创建采购订单
  ↓
到货后仓库收料
  ↓
收料完成后发料给生产
  ↓
MES继续分装 / 总装 / 调试
  ↓
发货 / 售后 / 验收
```

对应主链路：

```text
crm_contract
      ↓
mes_work_order
      ↓
engineering_material
      ↓
wms_inventory
      ↓
srm_purchase_request
      ↓
srm_purchase_order
      ↓
wms_receipt_order
      ↓
wms_issue_order
      ↓
mes_work_order_stage
```

---

# 二、V1 设计原则

1. **先简单跑通流程**：不做复杂 BOM、ECN、APS、MRP、工艺路线、批次追溯。
2. **合同驱动制造闭环**：以 `contract_id` 作为业务起点，以 `work_order_id` 作为制造全过程主线。
3. **所有关联字段使用明确命名**：不使用通用 `id`，统一使用 `customer_id`、`contract_id`、`work_order_id`、`supplier_id`、`part_id` 等。
4. **工程清算先替代 PDM-BOM**：V1 中 `engineering_material` 先作为简化版 BOM / 物料需求清单。
5. **采购、库存、收发料围绕工单闭环**：所有关键单据都保留 `work_order_id` 和 `contract_id`。
6. **AI 先做辅助，不做重型 Agent**：优先接入工单生成、物料识别、缺料分析、采购建议、进度摘要。

---

# 三、15 张核心表总览

| 序号 | 表名 | 所属模块 | 作用 |
|---|---|---|---|
| 1 | `crm_customer` | CRM | 客户主数据 |
| 2 | `crm_contact` | CRM | 客户联系人 |
| 3 | `crm_opportunity` | CRM | 商机/项目前置机会 |
| 4 | `crm_quote` | CRM | 报价单 |
| 5 | `crm_contract` | CRM | 合同，V1主流程起点 |
| 6 | `material_part` | 物料中心 | 物料/零件主数据 |
| 7 | `srm_supplier` | SRM | 供应商主数据 |
| 8 | `mes_work_order` | MES | 生产工单，制造主线 |
| 9 | `engineering_material` | 工程清算 | 工程物料需求/简化BOM |
| 10 | `wms_inventory` | WMS | 库存 |
| 11 | `srm_purchase_request` | SRM | 采购需求 |
| 12 | `srm_purchase_order` | SRM | 采购订单 |
| 13 | `wms_receipt_order` | WMS | 收料单 |
| 14 | `wms_issue_order` | WMS | 发料单 |
| 15 | `mes_work_order_stage` | MES | 工单阶段推进 |

> 实际开发建议另外补充 3 张明细表，见第十九章：`srm_purchase_order_item`、`wms_receipt_order_item`、`wms_issue_order_item`。

---

# 四、客户表 `crm_customer`

| 字段名 | 类型 | 说明 |
|---|---|---|
| customer_id | bigint | 客户主键 |
| name | varchar | 客户名称 |
| short_name | varchar | 客户简称 |
| code | varchar | 客户编码 |
| type | enum | 客户类型：终端/集成商/代理商/供应商兼客户 |
| level | enum | 客户等级：A/B/C/D |
| industry | varchar | 行业分类 |
| province | varchar | 省 |
| city | varchar | 市 |
| district | varchar | 区 |
| address | varchar | 详细地址 |
| website | varchar | 官网 |
| status | enum | 潜在/合作/暂停/黑名单 |
| scale | json | 员工数、年营收、产线数量、工厂面积等 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

---

# 五、联系人表 `crm_contact`

| 字段名 | 类型 | 说明 |
|---|---|---|
| contact_id | bigint | 主键 |
| customer_id | bigint | 所属客户 |
| name | varchar | 联系人姓名 |
| phone | varchar | 手机 |
| email | varchar | 邮箱 |
| wechat | varchar | 微信 |
| position | varchar | 职位 |
| department | varchar | 部门 |
| decision_role | enum | 决策/影响/使用/采购 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
crm_customer.customer_id
      ↓
crm_contact.customer_id
```

---

# 六、商机表 `crm_opportunity`

| 字段名 | 类型 | 说明 |
|---|---|---|
| opportunity_id | bigint | 主键 |
| customer_id | bigint | 客户 |
| contact_id | bigint | 负责人联系人 |
| name | varchar | 商机名称/项目名称 |
| estimated_amount | decimal | 预计金额 |
| estimated_close_date | date | 预计签单日期 |
| source | enum | 项目来源：市场/客户推荐/老客户/其他 |
| stage | enum | 线索/需求沟通/方案设计/报价/商务谈判/待签约/已成交/已失败 |
| success_rate | decimal | 成功率 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
crm_customer.customer_id
      ↓
crm_opportunity.customer_id

crm_contact.contact_id
      ↓
crm_opportunity.contact_id
```

---

# 七、报价表 `crm_quote`

| 字段名 | 类型 | 说明 |
|---|---|---|
| quote_id | bigint | 主键 |
| opportunity_id | bigint | 商机 |
| customer_id | bigint | 客户 |
| version | int | 版本号 |
| total_amount | decimal | 总金额 |
| status | enum | 草稿/审批中/已发送/客户确认/废弃 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
crm_opportunity.opportunity_id
      ↓
crm_quote.opportunity_id

crm_customer.customer_id
      ↓
crm_quote.customer_id
```

---

# 八、合同表 `crm_contract`

> V1 主流程起点。合同确认后生成生产工单。

| 字段名 | 类型 | 说明 |
|---|---|---|
| contract_id | bigint | 主键 |
| opportunity_id | bigint | 商机 |
| customer_id | bigint | 客户 |
| quote_id | bigint | 报价单 |
| name | varchar | 合同名称 |
| amount | decimal | 合同金额 |
| signed_date | date | 签订时间 |
| delivery_date | date | 交付时间 |
| status | enum | 草稿/审批中/待签署/执行中/完成/终止 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
crm_opportunity.opportunity_id
      ↓
crm_contract.opportunity_id

crm_customer.customer_id
      ↓
crm_contract.customer_id

crm_quote.quote_id
      ↓
crm_contract.quote_id
```

---

# 九、物料主数据表 `material_part`

> 工程清算、采购、库存、收料、发料、生产共同引用的物料/零件主数据。

| 字段名 | 类型 | 说明 |
|---|---|---|
| part_id | bigint | 主键 |
| part_code | varchar | 物料编码 |
| part_name | varchar | 物料名称 |
| specification | varchar | 规格型号 |
| unit | varchar | 单位 |
| material | varchar | 材质 |
| category | enum | 机加件/钣金件/标准件/电气件/外购件/其他 |
| default_supplier_id | bigint | 默认供应商 |
| status | enum | 启用/停用 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
srm_supplier.supplier_id
      ↓
material_part.default_supplier_id
```

---

# 十、供应商表 `srm_supplier`

> 供应商主数据。采购需求、采购订单、收料单均可关联供应商。

| 字段名 | 类型 | 说明 |
|---|---|---|
| supplier_id | bigint | 主键 |
| supplier_code | varchar | 供应商编码 |
| supplier_name | varchar | 供应商名称 |
| short_name | varchar | 简称 |
| contact_name | varchar | 联系人 |
| phone | varchar | 联系电话 |
| email | varchar | 邮箱 |
| address | varchar | 地址 |
| level | enum | A/B/C/D |
| status | enum | 合作中/暂停合作/黑名单 |
| remark | text | 备注 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

---

# 十一、生产工单表 `mes_work_order`

> 合同生成生产工单。`work_order_id` 是 V1 制造全过程主线 ID。

| 字段名 | 类型 | 说明 |
|---|---|---|
| work_order_id | bigint | 主键 |
| contract_id | bigint | 合同 |
| customer_id | bigint | 客户 |
| opportunity_id | bigint | 商机 |
| work_order_code | varchar | 工单编号 |
| project_name | varchar | 项目名称 |
| product_name | varchar | 产品名称 |
| quantity | int | 生产数量 |
| current_stage | enum | 当前阶段 |
| progress | decimal | 当前进度百分比 |
| status | enum | 待生产/进行中/暂停/完成/取消 |
| plan_delivery_date | date | 计划交付时间 |
| actual_delivery_date | date | 实际交付时间 |
| responsible_user_id | user_id | 负责人 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
crm_contract.contract_id
      ↓
mes_work_order.contract_id
```

---

# 十二、工程物料清算表 `engineering_material`

> 根据工单生成物料需求。V1 中它先承担简化 BOM / 物料需求计划功能。

| 字段名 | 类型 | 说明 |
|---|---|---|
| engineering_material_id | bigint | 主键 |
| work_order_id | bigint | 工单 |
| contract_id | bigint | 合同 |
| part_id | bigint | 物料 |
| part_code | varchar | 物料编码快照 |
| part_name | varchar | 物料名称快照 |
| specification | varchar | 规格型号快照 |
| unit | varchar | 单位快照 |
| required_qty | decimal | 需求数量 |
| stock_qty | decimal | 库存数量 |
| shortage_qty | decimal | 缺料数量 |
| purchase_qty | decimal | 采购数量 |
| status | enum | 待检查/库存充足/部分缺料/全部缺料/已采购/已收料/已发料 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
mes_work_order.work_order_id
      ↓
engineering_material.work_order_id

crm_contract.contract_id
      ↓
engineering_material.contract_id

material_part.part_id
      ↓
engineering_material.part_id
```

---

# 十三、库存表 `wms_inventory`

> 仓库实时库存。工程清算时用它做库存对比。

| 字段名 | 类型 | 说明 |
|---|---|---|
| inventory_id | bigint | 主键 |
| part_id | bigint | 物料 |
| part_code | varchar | 物料编码快照 |
| part_name | varchar | 物料名称快照 |
| specification | varchar | 规格型号快照 |
| unit | varchar | 单位快照 |
| stock_qty | decimal | 库存数量 |
| available_qty | decimal | 可用库存 |
| locked_qty | decimal | 冻结库存 |
| location_code | varchar | 库位编码 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
material_part.part_id
      ↓
wms_inventory.part_id
```

库存对照逻辑：

```text
engineering_material.required_qty
      ↓
对比 wms_inventory.available_qty
      ↓
计算 shortage_qty / purchase_qty
```

---

# 十四、采购需求表 `srm_purchase_request`

> 工程清算发现缺料后，自动生成采购需求。

| 字段名 | 类型 | 说明 |
|---|---|---|
| purchase_request_id | bigint | 主键 |
| work_order_id | bigint | 工单 |
| contract_id | bigint | 合同 |
| engineering_material_id | bigint | 来源工程物料 |
| supplier_id | bigint | 建议供应商 |
| part_id | bigint | 物料 |
| part_code | varchar | 物料编码快照 |
| part_name | varchar | 物料名称快照 |
| specification | varchar | 规格型号快照 |
| unit | varchar | 单位快照 |
| request_qty | decimal | 申请数量 |
| status | enum | 待采购/采购中/已完成/取消 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
engineering_material.engineering_material_id
      ↓
srm_purchase_request.engineering_material_id

mes_work_order.work_order_id
      ↓
srm_purchase_request.work_order_id

srm_supplier.supplier_id
      ↓
srm_purchase_request.supplier_id

material_part.part_id
      ↓
srm_purchase_request.part_id
```

---

# 十五、采购订单表 `srm_purchase_order`

> 采购需求确认后，生成采购订单。

| 字段名 | 类型 | 说明 |
|---|---|---|
| purchase_order_id | bigint | 主键 |
| purchase_order_code | varchar | 采购单号 |
| purchase_request_id | bigint | 来源采购需求 |
| supplier_id | bigint | 供应商 |
| work_order_id | bigint | 工单 |
| contract_id | bigint | 合同 |
| status | enum | 草稿/已下单/部分到货/全部到货/关闭 |
| order_date | date | 下单日期 |
| expected_delivery_date | date | 预计到货日期 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
srm_purchase_request.purchase_request_id
      ↓
srm_purchase_order.purchase_request_id

srm_supplier.supplier_id
      ↓
srm_purchase_order.supplier_id

mes_work_order.work_order_id
      ↓
srm_purchase_order.work_order_id
```

---

# 十六、收料单表 `wms_receipt_order`

> 采购订单到货后，仓库收料入库。

| 字段名 | 类型 | 说明 |
|---|---|---|
| receipt_order_id | bigint | 主键 |
| purchase_order_id | bigint | 采购订单 |
| supplier_id | bigint | 供应商 |
| work_order_id | bigint | 工单 |
| contract_id | bigint | 合同 |
| receipt_status | enum | 待收料/部分收料/收料完成 |
| receipt_time | timestamp | 收料时间 |
| warehouse_user_id | user_id | 仓管员 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
srm_purchase_order.purchase_order_id
      ↓
wms_receipt_order.purchase_order_id

srm_supplier.supplier_id
      ↓
wms_receipt_order.supplier_id
```

---

# 十七、发料单表 `wms_issue_order`

> 库存齐套后，仓库发料给生产。

| 字段名 | 类型 | 说明 |
|---|---|---|
| issue_order_id | bigint | 主键 |
| work_order_id | bigint | 工单 |
| contract_id | bigint | 合同 |
| issue_status | enum | 待发料/部分发料/发料完成 |
| issue_time | timestamp | 发料时间 |
| warehouse_user_id | user_id | 仓管员 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

关联关系：

```text
mes_work_order.work_order_id
      ↓
wms_issue_order.work_order_id

crm_contract.contract_id
      ↓
wms_issue_order.contract_id
```

---

# 十八、工单阶段表 `mes_work_order_stage`

> 控制整个项目推进流程。V1 的项目进度看板主要基于这张表。

| 字段名 | 类型 | 说明 |
|---|---|---|
| work_order_stage_id | bigint | 主键 |
| work_order_id | bigint | 工单 |
| stage_code | varchar | 阶段编码 |
| stage_name | varchar | 阶段名称 |
| status | enum | WAIT/PROCESSING/FINISHED/PAUSE |
| responsible_user_id | user_id | 负责人 |
| start_time | timestamp | 开始时间 |
| end_time | timestamp | 结束时间 |
| remark | text | 备注 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

阶段编码：

```text
ENGINEERING      工程清算
PURCHASE         采购
RECEIPT          收料
ISSUE            发料
ASSEMBLY         分装
FINAL_ASSEMBLY   总装
TEST             调试
DELIVERY         发货
SERVICE          售后
ACCEPTANCE       验收
```

状态：

```text
WAIT          等待
PROCESSING    进行中
FINISHED      完成
PAUSE         暂停
```

---

# 十九、建议补充的 3 张明细表

> 这 3 张不计入 15 张核心表，但实际开发强烈建议补充，避免后续采购、收料、发料只能记录单头，无法追溯明细。

---

## 19.1 采购订单明细表 `srm_purchase_order_item`

| 字段名 | 类型 | 说明 |
|---|---|---|
| purchase_order_item_id | bigint | 主键 |
| purchase_order_id | bigint | 采购订单 |
| purchase_request_id | bigint | 来源采购需求 |
| work_order_id | bigint | 工单 |
| contract_id | bigint | 合同 |
| part_id | bigint | 物料 |
| part_code | varchar | 物料编码快照 |
| part_name | varchar | 物料名称快照 |
| specification | varchar | 规格型号快照 |
| unit | varchar | 单位快照 |
| purchase_qty | decimal | 采购数量 |
| price | decimal | 单价 |
| amount | decimal | 金额 |
| received_qty | decimal | 已收数量 |
| status | enum | 待采购/采购中/部分到货/全部到货/取消 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

---

## 19.2 收料单明细表 `wms_receipt_order_item`

| 字段名 | 类型 | 说明 |
|---|---|---|
| receipt_order_item_id | bigint | 主键 |
| receipt_order_id | bigint | 收料单 |
| purchase_order_id | bigint | 采购订单 |
| purchase_order_item_id | bigint | 来源采购订单明细 |
| work_order_id | bigint | 工单 |
| contract_id | bigint | 合同 |
| part_id | bigint | 物料 |
| part_code | varchar | 物料编码快照 |
| part_name | varchar | 物料名称快照 |
| specification | varchar | 规格型号快照 |
| unit | varchar | 单位快照 |
| receipt_qty | decimal | 收料数量 |
| status | enum | 待收料/部分收料/收料完成 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

---

## 19.3 发料单明细表 `wms_issue_order_item`

| 字段名 | 类型 | 说明 |
|---|---|---|
| issue_order_item_id | bigint | 主键 |
| issue_order_id | bigint | 发料单 |
| work_order_id | bigint | 工单 |
| contract_id | bigint | 合同 |
| engineering_material_id | bigint | 来源工程物料 |
| part_id | bigint | 物料 |
| part_code | varchar | 物料编码快照 |
| part_name | varchar | 物料名称快照 |
| specification | varchar | 规格型号快照 |
| unit | varchar | 单位快照 |
| issue_qty | decimal | 发料数量 |
| status | enum | 待发料/部分发料/发料完成 |
| created_by | user_id | 创建人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

---

# 二十、V1 状态推进规则

## 20.1 合同生成工单

```text
crm_contract.status = 执行中
      ↓
生成 mes_work_order
      ↓
初始化 mes_work_order_stage
```

初始化阶段：

```text
ENGINEERING      PROCESSING
PURCHASE         WAIT
RECEIPT          WAIT
ISSUE            WAIT
ASSEMBLY         WAIT
FINAL_ASSEMBLY   WAIT
TEST             WAIT
DELIVERY         WAIT
SERVICE          WAIT
ACCEPTANCE       WAIT
```

---

## 20.2 工程清算

```text
录入 / 导入 engineering_material
      ↓
查询 wms_inventory.available_qty
      ↓
计算 shortage_qty
      ↓
若缺料，生成 srm_purchase_request
      ↓
ENGINEERING 阶段完成
```

---

## 20.3 采购

```text
srm_purchase_request
      ↓
生成 srm_purchase_order
      ↓
生成 srm_purchase_order_item
      ↓
采购订单下单
      ↓
PURCHASE 阶段进行中
```

采购全部到货后：

```text
PURCHASE 阶段完成
RECEIPT 阶段开始
```

---

## 20.4 收料

```text
srm_purchase_order
      ↓
wms_receipt_order
      ↓
wms_receipt_order_item
      ↓
库存增加 wms_inventory.stock_qty / available_qty
```

收料完成后：

```text
RECEIPT 阶段完成
ISSUE 阶段开始
```

---

## 20.5 发料

```text
wms_issue_order
      ↓
wms_issue_order_item
      ↓
库存减少 wms_inventory.available_qty
      ↓
engineering_material.status = 已发料
```

发料完成后：

```text
ISSUE 阶段完成
ASSEMBLY 阶段开始
```

---

## 20.6 生产阶段推进

```text
ASSEMBLY         分装
      ↓
FINAL_ASSEMBLY   总装
      ↓
TEST             调试
      ↓
DELIVERY         发货
      ↓
SERVICE          售后
      ↓
ACCEPTANCE       验收
```

项目完成后：

```text
mes_work_order.status = 完成
crm_contract.status = 完成
```

---

# 二十一、AI 接入建议

第一阶段 AI 不需要做完整多 Agent，先围绕主流程做工具能力。

## 21.1 推荐优先做的 AI 能力

```text
1. 合同内容 → 自动生成工单草稿
2. Excel / BOM → 自动识别工程物料清单（excel 格式：	供应商	品名	规格/型号单位	"单机数量"	"需求总量"	材质	交期	备注）
3. 工程清单 → 自动查询库存并计算缺料
4. 缺料清单 → 自动生成采购需求草稿
5. 采购需求 → 推荐供应商
6. 工单状态 → 自动生成项目进度摘要
7. 异常状态 → 自动生成预警提醒
```

## 21.2 推荐 Tool 清单

```text
crm.contract.query
crm.contract.generateWorkOrder

mes.workOrder.query
mes.workOrder.create
mes.workOrder.stage.update

engineering.material.import
engineering.material.checkInventory
engineering.material.generatePurchaseRequest

wms.inventory.query
wms.receipt.create
wms.issue.create

srm.supplier.query
srm.purchaseRequest.create
srm.purchaseOrder.create
```

---

# 二十二、开发建议

## 22.1 第一阶段开发顺序

```text
1. crm_contract
2. mes_work_order
3. mes_work_order_stage
4. material_part
5. engineering_material
6. wms_inventory
7. srm_supplier
8. srm_purchase_request
9. srm_purchase_order
10. srm_purchase_order_item
11. wms_receipt_order
12. wms_receipt_order_item
13. wms_issue_order
14. wms_issue_order_item
15. 项目进度看板
```

## 22.2 第一版前端页面

```text
合同列表
合同详情
生成工单

工单列表
工单详情
项目进度看板

工程清算
物料导入
库存检查
缺料清单

采购需求
采购订单

收料单
发料单

生产阶段推进
```

## 22.3 不建议第一版做的内容

```text
复杂 CRM 全流程
完整 PDM
多级 BOM
ECN 工程变更
MRP 运算
APS 排产
复杂工艺路线
批次追溯
财务应收应付
完整售后工单
```

这些都可以放到 V2/V3。

---

# 二十三、最终定位

XTP V1 不是传统 ERP 的完整实现，而是一个：

```text
合同驱动的项目型制造流程闭环系统
```

它的核心价值是：

```text
让企业知道：
这个合同对应的项目现在做到哪一步？
缺什么料？
谁负责？
卡在哪里？
什么时候能交付？
```

后续再从这条主线逐步长出：(待定，先完成上面的基础任务)

```text
CRM
PDM
SRM
WMS
MES
LOGISTICS
SERVICE
FINANCE
AI
```
