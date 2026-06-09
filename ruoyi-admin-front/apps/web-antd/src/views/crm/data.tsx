import type { FormSchemaGetter } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { CrmEntity } from '#/api/crm/model';

import { DictEnum } from '@vben/constants';
import { getPopupContainer } from '@vben/utils';

import {
  crmContactApi,
  crmContractApi,
  crmCustomerApi,
  crmFollowRecordApi,
  crmOpportunityApi,
  crmPaymentPlanApi,
  crmQuoteApi,
} from '#/api/crm';
import { getDictOptions } from '#/utils/dict';
import { renderDict } from '#/utils/render';

type CrmApi<T = any> = {
  add: (data: Partial<T>) => Promise<void>;
  export: (data: Partial<T>) => Promise<Blob>;
  info: (id: number | string) => Promise<T>;
  list: (params?: Record<string, any>) => Promise<T[]>;
  options: (params?: Partial<T>) => Promise<T[]>;
  remove: (ids: (number | string)[]) => Promise<void>;
  update: (data: Partial<T>) => Promise<void>;
};

export interface CrmSelectOption {
  label: string;
  value: number | string;
}

export interface CrmLookups {
  contacts: CrmSelectOption[];
  contracts: CrmSelectOption[];
  customers: CrmSelectOption[];
  opportunities: CrmSelectOption[];
  quotes: CrmSelectOption[];
}

export interface CrmPageConfig<T = CrmEntity> {
  api: CrmApi<T>;
  columns: VxeGridProps['columns'];
  drawerSchema: (lookups: CrmLookups) => ReturnType<FormSchemaGetter>;
  exportName: string;
  permission: string;
  querySchema: (lookups: CrmLookups) => ReturnType<FormSchemaGetter>;
  rowKey: string;
  title: string;
}

const emptyLookups: CrmLookups = {
  contacts: [],
  contracts: [],
  customers: [],
  opportunities: [],
  quotes: [],
};

export function getEmptyLookups() {
  return emptyLookups;
}

function toOptions<T extends Record<string, any>>(
  list: T[],
  valueKey: string,
  labelGetter: (item: T) => string,
): CrmSelectOption[] {
  return list.map((item) => ({
    label: labelGetter(item),
    value: item[valueKey],
  }));
}

export async function loadCrmLookups(): Promise<CrmLookups> {
  const [customers, contacts, opportunities, quotes, contracts] =
    await Promise.all([
      crmCustomerApi.options(),
      crmContactApi.options(),
      crmOpportunityApi.options(),
      crmQuoteApi.options(),
      crmContractApi.options(),
    ]);
  return {
    contacts: toOptions(contacts, 'contactId', (item) =>
      [item.name, item.phone].filter(Boolean).join(' / '),
    ),
    contracts: toOptions(contracts, 'contractId', (item) => item.name),
    customers: toOptions(customers, 'customerId', (item) =>
      [item.code, item.name].filter(Boolean).join(' / '),
    ),
    opportunities: toOptions(
      opportunities,
      'opportunityId',
      (item) => item.name,
    ),
    quotes: toOptions(quotes, 'quoteId', (item) =>
      displayTextOr(
        item,
        ['opportunityName', 'customerCode', 'customerName'],
        '报价单',
      ),
    ),
  };
}

function selectProps(options: CrmSelectOption[]) {
  return {
    getPopupContainer,
    optionFilterProp: 'label',
    options,
    showSearch: true,
  };
}

function dictProps(dictType: string) {
  return {
    getPopupContainer,
    options: getDictOptions(dictType),
  };
}

function hiddenId(fieldName: string) {
  return {
    component: 'Input',
    dependencies: {
      show: () => false,
      triggerFields: [''],
    },
    fieldName,
    label: fieldName,
  };
}

function customerSelect(lookups: CrmLookups, rules?: string) {
  return {
    component: 'Select',
    componentProps: selectProps(lookups.customers),
    fieldName: 'customerId',
    label: '客户',
    rules,
  };
}

function contactSelect(lookups: CrmLookups) {
  return {
    component: 'Select',
    componentProps: selectProps(lookups.contacts),
    fieldName: 'contactId',
    label: '联系人',
  };
}

function opportunitySelect(lookups: CrmLookups) {
  return {
    component: 'Select',
    componentProps: selectProps(lookups.opportunities),
    fieldName: 'opportunityId',
    label: '商机',
  };
}

function quoteSelect(lookups: CrmLookups) {
  return {
    component: 'Select',
    componentProps: selectProps(lookups.quotes),
    fieldName: 'quoteId',
    label: '报价单',
  };
}

function contractSelect(lookups: CrmLookups) {
  return {
    component: 'Select',
    componentProps: selectProps(lookups.contracts),
    fieldName: 'contractId',
    label: '合同',
  };
}

function actionColumn(width = 160) {
  return {
    field: 'action',
    fixed: 'right' as const,
    resizable: false,
    slots: { default: 'action' },
    title: '操作',
    width,
  };
}

function displayText(row: Record<string, any>, fields: string[]) {
  return fields
    .map((field) => row[field])
    .filter(Boolean)
    .join(' / ');
}

function displayTextOr(
  row: Record<string, any>,
  fields: string[],
  fallback: string,
) {
  return displayText(row, fields) || fallback;
}

function displayColumn(
  field: string,
  title: string,
  fields: string[],
  width = 150,
) {
  return {
    field,
    slots: {
      default: ({ row }: any) => displayText(row, fields) || '-',
    },
    title,
    width,
  };
}

export const crmCustomerConfig: CrmPageConfig = {
  api: crmCustomerApi,
  columns: [
    { type: 'checkbox', width: 60 },
    { field: 'code', title: '客户编码', width: 150 },
    { field: 'name', title: '客户名称', minWidth: 180 },
    { field: 'shortName', title: '简称', width: 140 },
    {
      field: 'type',
      slots: {
        default: ({ row }) => renderDict(row.type, DictEnum.CRM_CUSTOMER_TYPE),
      },
      title: '类型',
      width: 130,
    },
    {
      field: 'level',
      slots: {
        default: ({ row }) =>
          renderDict(row.level, DictEnum.CRM_CUSTOMER_LEVEL),
      },
      title: '等级',
      width: 90,
    },
    { field: 'industry', title: '行业', width: 130 },
    {
      field: 'status',
      slots: {
        default: ({ row }) =>
          renderDict(row.status, DictEnum.CRM_CUSTOMER_STATUS),
      },
      title: '状态',
      width: 110,
    },
    { field: 'createTime', title: '创建时间', width: 170 },
    actionColumn(),
  ],
  drawerSchema: () => [
    hiddenId('customerId'),
    {
      component: 'Input',
      fieldName: 'name',
      label: '客户名称',
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'code',
      label: '客户编码',
      rules: 'required',
    },
    { component: 'Input', fieldName: 'shortName', label: '客户简称' },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_CUSTOMER_TYPE),
      fieldName: 'type',
      label: '客户类型',
      rules: 'selectRequired',
    },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_CUSTOMER_LEVEL),
      defaultValue: 'A',
      fieldName: 'level',
      label: '客户等级',
    },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_CUSTOMER_STATUS),
      defaultValue: 'potential',
      fieldName: 'status',
      label: '客户状态',
    },
    { component: 'Input', fieldName: 'industry', label: '行业' },
    { component: 'Input', fieldName: 'province', label: '省' },
    { component: 'Input', fieldName: 'city', label: '市' },
    { component: 'Input', fieldName: 'district', label: '区' },
    { component: 'Input', fieldName: 'website', label: '官网' },
    {
      component: 'Textarea',
      fieldName: 'address',
      formItemClass: 'col-span-2 items-start',
      label: '详细地址',
    },
    {
      component: 'Textarea',
      fieldName: 'scale',
      formItemClass: 'col-span-2 items-start',
      label: '规模信息',
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      formItemClass: 'col-span-2 items-start',
      label: '备注',
    },
  ],
  exportName: 'CRM客户',
  permission: 'crm:customer',
  querySchema: () => [
    { component: 'Input', fieldName: 'name', label: '客户名称' },
    { component: 'Input', fieldName: 'code', label: '客户编码' },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_CUSTOMER_STATUS),
      fieldName: 'status',
      label: '客户状态',
    },
    { component: 'RangePicker', fieldName: 'createTime', label: '创建时间' },
  ],
  rowKey: 'customerId',
  title: '客户列表',
};

export const crmContactConfig: CrmPageConfig = {
  api: crmContactApi,
  columns: [
    { type: 'checkbox', width: 60 },
    { field: 'name', title: '联系人', width: 140 },
    displayColumn('customerId', '客户', ['customerCode', 'customerName'], 180),
    { field: 'phone', title: '手机', width: 140 },
    { field: 'email', title: '邮箱', minWidth: 180 },
    { field: 'position', title: '职位', width: 130 },
    { field: 'department', title: '部门', width: 130 },
    {
      field: 'decisionRole',
      slots: {
        default: ({ row }) =>
          renderDict(row.decisionRole, DictEnum.CRM_DECISION_ROLE),
      },
      title: '决策角色',
      width: 120,
    },
    { field: 'createTime', title: '创建时间', width: 170 },
    actionColumn(),
  ],
  drawerSchema: (lookups) => [
    hiddenId('contactId'),
    customerSelect(lookups, 'selectRequired'),
    {
      component: 'Input',
      fieldName: 'name',
      label: '联系人',
      rules: 'required',
    },
    { component: 'Input', fieldName: 'phone', label: '手机' },
    { component: 'Input', fieldName: 'email', label: '邮箱' },
    { component: 'Input', fieldName: 'wechat', label: '微信' },
    { component: 'Input', fieldName: 'position', label: '职位' },
    { component: 'Input', fieldName: 'department', label: '部门' },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_DECISION_ROLE),
      fieldName: 'decisionRole',
      label: '决策角色',
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      formItemClass: 'col-span-2 items-start',
      label: '备注',
    },
  ],
  exportName: 'CRM联系人',
  permission: 'crm:contact',
  querySchema: (lookups) => [
    customerSelect(lookups, undefined),
    { component: 'Input', fieldName: 'name', label: '联系人' },
    { component: 'Input', fieldName: 'phone', label: '手机' },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_DECISION_ROLE),
      fieldName: 'decisionRole',
      label: '决策角色',
    },
  ],
  rowKey: 'contactId',
  title: '联系人列表',
};

export const crmOpportunityConfig: CrmPageConfig = {
  api: crmOpportunityApi,
  columns: [
    { type: 'checkbox', width: 60 },
    { field: 'name', title: '商机名称', minWidth: 180 },
    displayColumn('customerId', '客户', ['customerCode', 'customerName'], 180),
    displayColumn('contactId', '联系人', ['contactName'], 120),
    { field: 'estimatedAmount', title: '预计金额', width: 120 },
    { field: 'estimatedCloseDate', title: '预计签单', width: 120 },
    {
      field: 'source',
      slots: {
        default: ({ row }) =>
          renderDict(row.source, DictEnum.CRM_OPPORTUNITY_SOURCE),
      },
      title: '来源',
      width: 110,
    },
    {
      field: 'stage',
      slots: {
        default: ({ row }) =>
          renderDict(row.stage, DictEnum.CRM_OPPORTUNITY_STAGE),
      },
      title: '阶段',
      width: 130,
    },
    { field: 'successRate', title: '成功率', width: 100 },
    { field: 'createTime', title: '创建时间', width: 170 },
    actionColumn(),
  ],
  drawerSchema: (lookups) => [
    hiddenId('opportunityId'),
    customerSelect(lookups, 'selectRequired'),
    contactSelect(lookups),
    {
      component: 'Input',
      fieldName: 'name',
      label: '商机名称',
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { min: 0 },
      fieldName: 'estimatedAmount',
      label: '预计金额',
    },
    {
      component: 'DatePicker',
      componentProps: { getPopupContainer, valueFormat: 'YYYY-MM-DD' },
      fieldName: 'estimatedCloseDate',
      label: '预计签单',
    },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_OPPORTUNITY_SOURCE),
      fieldName: 'source',
      label: '项目来源',
    },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_OPPORTUNITY_STAGE),
      fieldName: 'stage',
      label: '销售阶段',
      rules: 'selectRequired',
    },
    {
      component: 'InputNumber',
      componentProps: { max: 100, min: 0 },
      fieldName: 'successRate',
      label: '成功率',
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      formItemClass: 'col-span-2 items-start',
      label: '备注',
    },
  ],
  exportName: 'CRM商机',
  permission: 'crm:opportunity',
  querySchema: (lookups) => [
    customerSelect(lookups, undefined),
    { component: 'Input', fieldName: 'name', label: '商机名称' },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_OPPORTUNITY_STAGE),
      fieldName: 'stage',
      label: '销售阶段',
    },
  ],
  rowKey: 'opportunityId',
  title: '商机列表',
};

export const crmFollowRecordConfig: CrmPageConfig = {
  api: crmFollowRecordApi,
  columns: [
    { type: 'checkbox', width: 60 },
    { field: 'followTime', title: '跟进时间', width: 170 },
    displayColumn('customerId', '客户', ['customerCode', 'customerName'], 180),
    displayColumn('opportunityId', '商机', ['opportunityName'], 160),
    {
      field: 'followMethod',
      slots: {
        default: ({ row }) =>
          renderDict(row.followMethod, DictEnum.CRM_FOLLOW_METHOD),
      },
      title: '方式',
      width: 110,
    },
    { field: 'content', title: '内容', minWidth: 240 },
    {
      field: 'result',
      slots: {
        default: ({ row }) =>
          renderDict(row.result, DictEnum.CRM_FOLLOW_RESULT),
      },
      title: '结果',
      width: 120,
    },
    { field: 'nextFollowTime', title: '下次跟进', width: 170 },
    actionColumn(),
  ],
  drawerSchema: (lookups) => [
    hiddenId('followId'),
    customerSelect(lookups, 'selectRequired'),
    opportunitySelect(lookups),
    contactSelect(lookups),
    {
      component: 'DatePicker',
      componentProps: {
        getPopupContainer,
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'followTime',
      label: '跟进时间',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_FOLLOW_METHOD),
      fieldName: 'followMethod',
      label: '跟进方式',
    },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_FOLLOW_RESULT),
      fieldName: 'result',
      label: '跟进结果',
    },
    {
      component: 'DatePicker',
      componentProps: {
        getPopupContainer,
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'nextFollowTime',
      label: '下次跟进',
    },
    {
      component: 'Textarea',
      fieldName: 'content',
      formItemClass: 'col-span-2 items-start',
      label: '跟进内容',
      rules: 'required',
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      formItemClass: 'col-span-2 items-start',
      label: '备注',
    },
  ],
  exportName: 'CRM跟进记录',
  permission: 'crm:followRecord',
  querySchema: (lookups) => [
    customerSelect(lookups, undefined),
    opportunitySelect(lookups),
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_FOLLOW_RESULT),
      fieldName: 'result',
      label: '跟进结果',
    },
    { component: 'RangePicker', fieldName: 'followTime', label: '跟进时间' },
  ],
  rowKey: 'followId',
  title: '跟进记录',
};

export const crmQuoteConfig: CrmPageConfig = {
  api: crmQuoteApi,
  columns: [
    { type: 'checkbox', width: 60 },
    displayColumn(
      'quoteId',
      '报价',
      ['opportunityName', 'customerCode', 'customerName'],
      180,
    ),
    displayColumn('customerId', '客户', ['customerCode', 'customerName'], 180),
    displayColumn('opportunityId', '商机', ['opportunityName'], 160),
    { field: 'version', title: '版本', width: 80 },
    { field: 'totalAmount', title: '总金额', width: 120 },
    {
      field: 'status',
      slots: {
        default: ({ row }) => renderDict(row.status, DictEnum.CRM_QUOTE_STATUS),
      },
      title: '状态',
      width: 120,
    },
    { field: 'createTime', title: '创建时间', width: 170 },
    actionColumn(),
  ],
  drawerSchema: (lookups) => [
    hiddenId('quoteId'),
    customerSelect(lookups, 'selectRequired'),
    opportunitySelect(lookups),
    {
      component: 'InputNumber',
      componentProps: { min: 1 },
      defaultValue: 1,
      fieldName: 'version',
      label: '版本',
    },
    {
      component: 'InputNumber',
      componentProps: { min: 0 },
      fieldName: 'totalAmount',
      label: '总金额',
    },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_QUOTE_STATUS),
      defaultValue: 'draft',
      fieldName: 'status',
      label: '报价状态',
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      formItemClass: 'col-span-2 items-start',
      label: '备注',
    },
  ],
  exportName: 'CRM报价',
  permission: 'crm:quote',
  querySchema: (lookups) => [
    customerSelect(lookups, undefined),
    opportunitySelect(lookups),
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_QUOTE_STATUS),
      fieldName: 'status',
      label: '报价状态',
    },
  ],
  rowKey: 'quoteId',
  title: '报价列表',
};

export const crmContractConfig: CrmPageConfig = {
  api: crmContractApi,
  columns: [
    { type: 'checkbox', width: 60 },
    { field: 'name', title: '合同名称', minWidth: 180 },
    displayColumn('customerId', '客户', ['customerCode', 'customerName'], 180),
    { field: 'amount', title: '合同金额', width: 120 },
    { field: 'signedDate', title: '签订日期', width: 120 },
    { field: 'deliveryDate', title: '交付日期', width: 120 },
    {
      field: 'status',
      slots: {
        default: ({ row }) =>
          renderDict(row.status, DictEnum.CRM_CONTRACT_STATUS),
      },
      title: '状态',
      width: 120,
    },
    { field: 'createTime', title: '创建时间', width: 170 },
    actionColumn(),
  ],
  drawerSchema: (lookups) => [
    hiddenId('contractId'),
    customerSelect(lookups, 'selectRequired'),
    opportunitySelect(lookups),
    quoteSelect(lookups),
    {
      component: 'Input',
      fieldName: 'name',
      label: '合同名称',
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { min: 0 },
      fieldName: 'amount',
      label: '合同金额',
    },
    {
      component: 'DatePicker',
      componentProps: { getPopupContainer, valueFormat: 'YYYY-MM-DD' },
      fieldName: 'signedDate',
      label: '签订日期',
    },
    {
      component: 'DatePicker',
      componentProps: { getPopupContainer, valueFormat: 'YYYY-MM-DD' },
      fieldName: 'deliveryDate',
      label: '交付日期',
    },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_CONTRACT_STATUS),
      defaultValue: 'draft',
      fieldName: 'status',
      label: '合同状态',
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      formItemClass: 'col-span-2 items-start',
      label: '备注',
    },
  ],
  exportName: 'CRM合同',
  permission: 'crm:contract',
  querySchema: (lookups) => [
    customerSelect(lookups, undefined),
    { component: 'Input', fieldName: 'name', label: '合同名称' },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_CONTRACT_STATUS),
      fieldName: 'status',
      label: '合同状态',
    },
  ],
  rowKey: 'contractId',
  title: '合同列表',
};

export const crmPaymentPlanConfig: CrmPageConfig = {
  api: crmPaymentPlanApi,
  columns: [
    { type: 'checkbox', width: 60 },
    { field: 'stageName', title: '付款节点', minWidth: 160 },
    displayColumn('customerId', '客户', ['customerCode', 'customerName'], 180),
    displayColumn('contractId', '合同', ['contractName'], 180),
    { field: 'amount', title: '金额', width: 120 },
    { field: 'plannedDate', title: '计划收款', width: 120 },
    {
      field: 'status',
      slots: {
        default: ({ row }) =>
          renderDict(row.status, DictEnum.CRM_PAYMENT_STATUS),
      },
      title: '状态',
      width: 120,
    },
    { field: 'createTime', title: '创建时间', width: 170 },
    actionColumn(),
  ],
  drawerSchema: (lookups) => [
    hiddenId('paymentId'),
    customerSelect(lookups, 'selectRequired'),
    contractSelect(lookups),
    opportunitySelect(lookups),
    {
      component: 'Input',
      fieldName: 'stageName',
      label: '付款节点',
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { min: 0 },
      fieldName: 'amount',
      label: '金额',
    },
    {
      component: 'DatePicker',
      componentProps: { getPopupContainer, valueFormat: 'YYYY-MM-DD' },
      fieldName: 'plannedDate',
      label: '计划收款',
    },
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_PAYMENT_STATUS),
      defaultValue: 'not_due',
      fieldName: 'status',
      label: '回款状态',
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      formItemClass: 'col-span-2 items-start',
      label: '备注',
    },
  ],
  exportName: 'CRM回款计划',
  permission: 'crm:paymentPlan',
  querySchema: (lookups) => [
    customerSelect(lookups, undefined),
    contractSelect(lookups),
    {
      component: 'Select',
      componentProps: dictProps(DictEnum.CRM_PAYMENT_STATUS),
      fieldName: 'status',
      label: '回款状态',
    },
  ],
  rowKey: 'paymentId',
  title: '回款计划',
};
