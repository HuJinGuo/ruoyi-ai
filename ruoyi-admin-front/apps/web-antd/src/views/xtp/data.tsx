import type { XtpEntity } from '#/api/xtp/model';

import { DictEnum } from '@vben/constants';
import { getPopupContainer } from '@vben/utils';

import { crmContractApi } from '#/api/crm';
import {
  materialPartApi,
  srmSupplierApi,
  mesWorkOrderApi,
  engineeringMaterialApi,
  wmsInventoryApi,
  srmPurchaseRequestApi,
  srmPurchaseOrderApi,
  srmPurchaseOrderItemApi,
  wmsReceiptOrderApi,
  wmsReceiptOrderItemApi,
  wmsIssueOrderApi,
  wmsIssueOrderItemApi,
  mesWorkOrderStageApi,
} from '#/api/xtp';
import { getDictOptions } from '#/utils/dict';
import { renderDict } from '#/utils/render';
type XtpApi<T = any> = {
  add: (data: Partial<T>) => Promise<void>;
  export: (data: Partial<T>) => Promise<Blob>;
  info: (id: number | string) => Promise<T>;
  list: (params?: Record<string, any>) => Promise<T[]>;
  options: (params?: Partial<T>) => Promise<T[]>;
  remove: (ids: (number | string)[]) => Promise<void>;
  update: (data: Partial<T>) => Promise<void>;
};

export interface XtpSelectOption { label: string; value: number | string }
export interface XtpLookups {
  contracts: XtpSelectOption[];
  engineeringMaterials: XtpSelectOption[];
  issueOrders: XtpSelectOption[];
  parts: XtpSelectOption[];
  purchaseOrderItems: XtpSelectOption[];
  purchaseOrders: XtpSelectOption[];
  purchaseRequests: XtpSelectOption[];
  receiptOrders: XtpSelectOption[];
  suppliers: XtpSelectOption[];
  workOrders: XtpSelectOption[];
}
export interface XtpPageConfig<T = XtpEntity> {
  api: XtpApi<T>;
  columns: any[];
  drawerSchema: (lookups: XtpLookups) => any[];
  exportName: string;
  permission: string;
  querySchema: (lookups: XtpLookups) => any[];
  rowKey: string;
  title: string;
}
const emptyLookups: XtpLookups = { contracts: [], engineeringMaterials: [], issueOrders: [], parts: [], purchaseOrderItems: [], purchaseOrders: [], purchaseRequests: [], receiptOrders: [], suppliers: [], workOrders: [] };
export function getEmptyLookups() { return emptyLookups; }
function toOptions<T extends Record<string, any>>(list: T[], valueKey: string, labelGetter: (item: T) => string): XtpSelectOption[] {
  return list.map((item) => ({ label: labelGetter(item), value: item[valueKey] }));
}
export async function loadXtpLookups(): Promise<XtpLookups> {
  const [contracts, parts, suppliers, workOrders, engineeringMaterials, purchaseRequests, purchaseOrders, purchaseOrderItems, receiptOrders, issueOrders] = await Promise.all([
    crmContractApi.options(), materialPartApi.options(), srmSupplierApi.options(), mesWorkOrderApi.options(), engineeringMaterialApi.options(), srmPurchaseRequestApi.options(), srmPurchaseOrderApi.options(), srmPurchaseOrderItemApi.options(), wmsReceiptOrderApi.options(), wmsIssueOrderApi.options(),
  ]);
  return {
    contracts: toOptions(contracts, 'contractId', (item) => item.name),
    engineeringMaterials: toOptions(engineeringMaterials, 'engineeringMaterialId', (item) => [item.partCode, item.partName].filter(Boolean).join(' / ')),
    issueOrders: toOptions(issueOrders, 'issueOrderId', (item) => `发料单 #${item.issueOrderId}`),
    parts: toOptions(parts, 'partId', (item) => [item.partCode, item.partName].filter(Boolean).join(' / ')),
    purchaseOrderItems: toOptions(purchaseOrderItems, 'purchaseOrderItemId', (item) => `采购明细 #${item.purchaseOrderItemId}`),
    purchaseOrders: toOptions(purchaseOrders, 'purchaseOrderId', (item) => item.purchaseOrderCode || `采购订单 #${item.purchaseOrderId}`),
    purchaseRequests: toOptions(purchaseRequests, 'purchaseRequestId', (item) => `采购需求 #${item.purchaseRequestId}`),
    receiptOrders: toOptions(receiptOrders, 'receiptOrderId', (item) => `收料单 #${item.receiptOrderId}`),
    suppliers: toOptions(suppliers, 'supplierId', (item) => [item.supplierCode, item.supplierName].filter(Boolean).join(' / ')),
    workOrders: toOptions(workOrders, 'workOrderId', (item) => [item.workOrderCode, item.projectName].filter(Boolean).join(' / ')),
  };
}
function selectProps(options: XtpSelectOption[]) { return { getPopupContainer, optionFilterProp: 'label', options, showSearch: true }; }
function dictProps(dictType: string) { return { getPopupContainer, options: getDictOptions(dictType) }; }
function lookupProps(lookups: XtpLookups, key: keyof XtpLookups) { return selectProps(lookups[key]); }
function actionColumn(width = 160) { return { field: 'action', fixed: 'right' as const, resizable: false, slots: { default: 'action' }, title: '操作', width }; }
function fieldSchema(fieldName: string, label: string, component: string, dictOrLookup: string | null, required = false, lookups: XtpLookups) {
  const schema: Record<string, any> = { component, fieldName, label };
  if (required) schema.rules = 'required';
  if (component === 'Textarea') schema.formItemClass = 'col-span-2 items-start';
  if (component === 'InputNumber') schema.componentProps = { min: 0 };
  if (component === 'Select' && dictOrLookup) {
    if (dictOrLookup.startsWith('XTP_')) schema.componentProps = dictProps(DictEnum[dictOrLookup as keyof typeof DictEnum]);
    else {
      const map: Record<string, keyof XtpLookups> = { contract: 'contracts', engineeringMaterial: 'engineeringMaterials', issueOrder: 'issueOrders', part: 'parts', purchaseOrder: 'purchaseOrders', purchaseOrderItem: 'purchaseOrderItems', purchaseRequest: 'purchaseRequests', receiptOrder: 'receiptOrders', supplier: 'suppliers', workOrder: 'workOrders' };
      const lookupKey = map[dictOrLookup];
      if (lookupKey) schema.componentProps = lookupProps(lookups, lookupKey);
    }
  }
  return schema;
}
function column(field: string, title: string, dict?: keyof typeof DictEnum) {
  return dict ? { field, slots: { default: ({ row }: any) => renderDict(row[field], DictEnum[dict]) }, title, width: 130 } : { field, title, minWidth: 130 };
}
function createConfig(meta: any): XtpPageConfig {
  const fields = meta.fields as any[];
  const queryFields = fields.filter((f) => ['Input', 'Select'].includes(f.component)).slice(0, 3);
  return {
    api: meta.api,
    columns: [{ type: 'checkbox', width: 60 }, column(meta.rowKey, 'ID'), ...fields.slice(0, 8).map((f) => column(f.name, f.label, f.dict && f.dict.startsWith('XTP_') ? f.dict : undefined)), actionColumn()],
    drawerSchema: (lookups) => fields.map((f) => fieldSchema(f.name, f.label, f.component, f.dict, f.required, lookups)),
    exportName: meta.title,
    permission: meta.permission,
    querySchema: (lookups) => [...queryFields.map((f) => fieldSchema(f.name, f.label, f.component, f.dict, false, lookups)), { component: 'RangePicker', fieldName: 'createTime', label: '创建时间' }],
    rowKey: meta.rowKey,
    title: meta.title,
  };
}
export const materialPartConfig = createConfig({ api: materialPartApi, rowKey: 'materialPartId', title: '物料管理', permission: 'material:part', fields: [
      { name: 'partCode', label: '物料编码', component: 'Input', dict: null, required: true },
      { name: 'partName', label: '物料名称', component: 'Input', dict: null, required: true },
      { name: 'specification', label: '规格型号', component: 'Input', dict: null, required: false },
      { name: 'unit', label: '单位', component: 'Input', dict: null, required: false },
      { name: 'material', label: '材质', component: 'Input', dict: null, required: false },
      { name: 'category', label: '分类', component: 'Select', dict: 'XTP_MATERIAL_CATEGORY', required: false },
      { name: 'defaultSupplierId', label: '默认供应商', component: 'Select', dict: 'supplier', required: false },
      { name: 'status', label: '状态', component: 'Select', dict: 'XTP_ENABLE_STATUS', required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const srmSupplierConfig = createConfig({ api: srmSupplierApi, rowKey: 'supplierId', title: '供应商管理', permission: 'srm:supplier', fields: [
      { name: 'supplierCode', label: '供应商编码', component: 'Input', dict: null, required: true },
      { name: 'supplierName', label: '供应商名称', component: 'Input', dict: null, required: true },
      { name: 'shortName', label: '简称', component: 'Input', dict: null, required: false },
      { name: 'contactName', label: '联系人', component: 'Input', dict: null, required: false },
      { name: 'phone', label: '联系电话', component: 'Input', dict: null, required: false },
      { name: 'email', label: '邮箱', component: 'Input', dict: null, required: false },
      { name: 'address', label: '地址', component: 'Textarea', dict: null, required: false },
      { name: 'level', label: '等级', component: 'Select', dict: 'XTP_SUPPLIER_LEVEL', required: false },
      { name: 'status', label: '状态', component: 'Select', dict: 'XTP_SUPPLIER_STATUS', required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const mesWorkOrderConfig = createConfig({ api: mesWorkOrderApi, rowKey: 'workOrderId', title: '生产工单', permission: 'mes:workOrder', fields: [
      { name: 'contractId', label: '合同', component: 'Select', dict: 'contract', required: true },
      { name: 'customerId', label: '客户ID', component: 'InputNumber', dict: null, required: false },
      { name: 'opportunityId', label: '商机ID', component: 'InputNumber', dict: null, required: false },
      { name: 'workOrderCode', label: '工单编号', component: 'Input', dict: null, required: true },
      { name: 'projectName', label: '项目名称', component: 'Input', dict: null, required: true },
      { name: 'productName', label: '产品名称', component: 'Input', dict: null, required: false },
      { name: 'quantity', label: '数量', component: 'InputNumber', dict: null, required: false },
      { name: 'currentStage', label: '当前阶段', component: 'Input', dict: null, required: false },
      { name: 'progress', label: '进度', component: 'InputNumber', dict: null, required: false },
      { name: 'status', label: '状态', component: 'Select', dict: 'XTP_WORK_ORDER_STATUS', required: false },
      { name: 'planDeliveryDate', label: '计划交付', component: 'DatePicker', dict: null, required: false },
      { name: 'actualDeliveryDate', label: '实际交付', component: 'DatePicker', dict: null, required: false },
      { name: 'responsibleUserId', label: '负责人ID', component: 'InputNumber', dict: null, required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const engineeringMaterialConfig = createConfig({ api: engineeringMaterialApi, rowKey: 'engineeringMaterialId', title: '工程清算', permission: 'engineering:material', fields: [
      { name: 'workOrderId', label: '工单', component: 'Select', dict: 'workOrder', required: true },
      { name: 'contractId', label: '合同', component: 'Select', dict: 'contract', required: false },
      { name: 'partId', label: '物料', component: 'Select', dict: 'part', required: false },
      { name: 'partCode', label: '物料编码', component: 'Input', dict: null, required: false },
      { name: 'partName', label: '物料名称', component: 'Input', dict: null, required: false },
      { name: 'specification', label: '规格型号', component: 'Input', dict: null, required: false },
      { name: 'unit', label: '单位', component: 'Input', dict: null, required: false },
      { name: 'requiredQty', label: '需求数量', component: 'InputNumber', dict: null, required: false },
      { name: 'stockQty', label: '库存数量', component: 'InputNumber', dict: null, required: false },
      { name: 'shortageQty', label: '缺料数量', component: 'InputNumber', dict: null, required: false },
      { name: 'purchaseQty', label: '采购数量', component: 'InputNumber', dict: null, required: false },
      { name: 'status', label: '状态', component: 'Select', dict: 'XTP_ENGINEERING_MATERIAL_STATUS', required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const wmsInventoryConfig = createConfig({ api: wmsInventoryApi, rowKey: 'inventoryId', title: '库存管理', permission: 'wms:inventory', fields: [
      { name: 'partId', label: '物料', component: 'Select', dict: 'part', required: true },
      { name: 'partCode', label: '物料编码', component: 'Input', dict: null, required: false },
      { name: 'partName', label: '物料名称', component: 'Input', dict: null, required: false },
      { name: 'specification', label: '规格型号', component: 'Input', dict: null, required: false },
      { name: 'unit', label: '单位', component: 'Input', dict: null, required: false },
      { name: 'stockQty', label: '库存数量', component: 'InputNumber', dict: null, required: false },
      { name: 'availableQty', label: '可用库存', component: 'InputNumber', dict: null, required: false },
      { name: 'lockedQty', label: '冻结库存', component: 'InputNumber', dict: null, required: false },
      { name: 'locationCode', label: '库位', component: 'Input', dict: null, required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const srmPurchaseRequestConfig = createConfig({ api: srmPurchaseRequestApi, rowKey: 'purchaseRequestId', title: '采购需求', permission: 'srm:purchaseRequest', fields: [
      { name: 'workOrderId', label: '工单', component: 'Select', dict: 'workOrder', required: true },
      { name: 'contractId', label: '合同', component: 'Select', dict: 'contract', required: false },
      { name: 'engineeringMaterialId', label: '工程物料', component: 'Select', dict: 'engineeringMaterial', required: false },
      { name: 'supplierId', label: '供应商', component: 'Select', dict: 'supplier', required: false },
      { name: 'partId', label: '物料', component: 'Select', dict: 'part', required: false },
      { name: 'partCode', label: '物料编码', component: 'Input', dict: null, required: false },
      { name: 'partName', label: '物料名称', component: 'Input', dict: null, required: false },
      { name: 'specification', label: '规格型号', component: 'Input', dict: null, required: false },
      { name: 'unit', label: '单位', component: 'Input', dict: null, required: false },
      { name: 'requestQty', label: '申请数量', component: 'InputNumber', dict: null, required: false },
      { name: 'status', label: '状态', component: 'Select', dict: 'XTP_PURCHASE_REQUEST_STATUS', required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const srmPurchaseOrderConfig = createConfig({ api: srmPurchaseOrderApi, rowKey: 'purchaseOrderId', title: '采购订单', permission: 'srm:purchaseOrder', fields: [
      { name: 'purchaseOrderCode', label: '采购单号', component: 'Input', dict: null, required: true },
      { name: 'purchaseRequestId', label: '采购需求', component: 'Select', dict: 'purchaseRequest', required: false },
      { name: 'supplierId', label: '供应商', component: 'Select', dict: 'supplier', required: false },
      { name: 'workOrderId', label: '工单', component: 'Select', dict: 'workOrder', required: false },
      { name: 'contractId', label: '合同', component: 'Select', dict: 'contract', required: false },
      { name: 'status', label: '状态', component: 'Select', dict: 'XTP_PURCHASE_ORDER_STATUS', required: false },
      { name: 'orderDate', label: '下单日期', component: 'DatePicker', dict: null, required: false },
      { name: 'expectedDeliveryDate', label: '预计到货', component: 'DatePicker', dict: null, required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const srmPurchaseOrderItemConfig = createConfig({ api: srmPurchaseOrderItemApi, rowKey: 'purchaseOrderItemId', title: '采购明细', permission: 'srm:purchaseOrderItem', fields: [
      { name: 'purchaseOrderId', label: '采购订单', component: 'Select', dict: 'purchaseOrder', required: true },
      { name: 'purchaseRequestId', label: '采购需求', component: 'Select', dict: 'purchaseRequest', required: false },
      { name: 'workOrderId', label: '工单', component: 'Select', dict: 'workOrder', required: false },
      { name: 'contractId', label: '合同', component: 'Select', dict: 'contract', required: false },
      { name: 'partId', label: '物料', component: 'Select', dict: 'part', required: false },
      { name: 'partCode', label: '物料编码', component: 'Input', dict: null, required: false },
      { name: 'partName', label: '物料名称', component: 'Input', dict: null, required: false },
      { name: 'specification', label: '规格型号', component: 'Input', dict: null, required: false },
      { name: 'unit', label: '单位', component: 'Input', dict: null, required: false },
      { name: 'purchaseQty', label: '采购数量', component: 'InputNumber', dict: null, required: false },
      { name: 'price', label: '单价', component: 'InputNumber', dict: null, required: false },
      { name: 'amount', label: '金额', component: 'InputNumber', dict: null, required: false },
      { name: 'receivedQty', label: '已收数量', component: 'InputNumber', dict: null, required: false },
      { name: 'status', label: '状态', component: 'Select', dict: 'XTP_PURCHASE_ORDER_STATUS', required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const wmsReceiptOrderConfig = createConfig({ api: wmsReceiptOrderApi, rowKey: 'receiptOrderId', title: '收料单', permission: 'wms:receiptOrder', fields: [
      { name: 'purchaseOrderId', label: '采购订单', component: 'Select', dict: 'purchaseOrder', required: true },
      { name: 'supplierId', label: '供应商', component: 'Select', dict: 'supplier', required: false },
      { name: 'workOrderId', label: '工单', component: 'Select', dict: 'workOrder', required: false },
      { name: 'contractId', label: '合同', component: 'Select', dict: 'contract', required: false },
      { name: 'receiptStatus', label: '收料状态', component: 'Select', dict: 'XTP_RECEIPT_STATUS', required: false },
      { name: 'receiptTime', label: '收料时间', component: 'DatePicker', dict: null, required: false },
      { name: 'warehouseUserId', label: '仓管员ID', component: 'InputNumber', dict: null, required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const wmsReceiptOrderItemConfig = createConfig({ api: wmsReceiptOrderItemApi, rowKey: 'receiptOrderItemId', title: '收料明细', permission: 'wms:receiptOrderItem', fields: [
      { name: 'receiptOrderId', label: '收料单', component: 'Select', dict: 'receiptOrder', required: true },
      { name: 'purchaseOrderId', label: '采购订单', component: 'Select', dict: 'purchaseOrder', required: false },
      { name: 'purchaseOrderItemId', label: '采购明细', component: 'Select', dict: 'purchaseOrderItem', required: false },
      { name: 'workOrderId', label: '工单', component: 'Select', dict: 'workOrder', required: false },
      { name: 'contractId', label: '合同', component: 'Select', dict: 'contract', required: false },
      { name: 'partId', label: '物料', component: 'Select', dict: 'part', required: false },
      { name: 'partCode', label: '物料编码', component: 'Input', dict: null, required: false },
      { name: 'partName', label: '物料名称', component: 'Input', dict: null, required: false },
      { name: 'specification', label: '规格型号', component: 'Input', dict: null, required: false },
      { name: 'unit', label: '单位', component: 'Input', dict: null, required: false },
      { name: 'receiptQty', label: '收料数量', component: 'InputNumber', dict: null, required: false },
      { name: 'status', label: '状态', component: 'Select', dict: 'XTP_RECEIPT_STATUS', required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const wmsIssueOrderConfig = createConfig({ api: wmsIssueOrderApi, rowKey: 'issueOrderId', title: '发料单', permission: 'wms:issueOrder', fields: [
      { name: 'workOrderId', label: '工单', component: 'Select', dict: 'workOrder', required: true },
      { name: 'contractId', label: '合同', component: 'Select', dict: 'contract', required: false },
      { name: 'issueStatus', label: '发料状态', component: 'Select', dict: 'XTP_ISSUE_STATUS', required: false },
      { name: 'issueTime', label: '发料时间', component: 'DatePicker', dict: null, required: false },
      { name: 'warehouseUserId', label: '仓管员ID', component: 'InputNumber', dict: null, required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const wmsIssueOrderItemConfig = createConfig({ api: wmsIssueOrderItemApi, rowKey: 'issueOrderItemId', title: '发料明细', permission: 'wms:issueOrderItem', fields: [
      { name: 'issueOrderId', label: '发料单', component: 'Select', dict: 'issueOrder', required: true },
      { name: 'workOrderId', label: '工单', component: 'Select', dict: 'workOrder', required: false },
      { name: 'contractId', label: '合同', component: 'Select', dict: 'contract', required: false },
      { name: 'engineeringMaterialId', label: '工程物料', component: 'Select', dict: 'engineeringMaterial', required: false },
      { name: 'partId', label: '物料', component: 'Select', dict: 'part', required: false },
      { name: 'partCode', label: '物料编码', component: 'Input', dict: null, required: false },
      { name: 'partName', label: '物料名称', component: 'Input', dict: null, required: false },
      { name: 'specification', label: '规格型号', component: 'Input', dict: null, required: false },
      { name: 'unit', label: '单位', component: 'Input', dict: null, required: false },
      { name: 'issueQty', label: '发料数量', component: 'InputNumber', dict: null, required: false },
      { name: 'status', label: '状态', component: 'Select', dict: 'XTP_ISSUE_STATUS', required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
export const mesWorkOrderStageConfig = createConfig({ api: mesWorkOrderStageApi, rowKey: 'workOrderStageId', title: '工单阶段', permission: 'mes:workOrderStage', fields: [
      { name: 'workOrderId', label: '工单', component: 'Select', dict: 'workOrder', required: true },
      { name: 'stageCode', label: '阶段编码', component: 'Input', dict: null, required: true },
      { name: 'stageName', label: '阶段名称', component: 'Input', dict: null, required: true },
      { name: 'status', label: '状态', component: 'Select', dict: 'XTP_STAGE_STATUS', required: false },
      { name: 'responsibleUserId', label: '负责人ID', component: 'InputNumber', dict: null, required: false },
      { name: 'startTime', label: '开始时间', component: 'DatePicker', dict: null, required: false },
      { name: 'endTime', label: '结束时间', component: 'DatePicker', dict: null, required: false },
      { name: 'remark', label: '备注', component: 'Textarea', dict: null, required: false }
] });
