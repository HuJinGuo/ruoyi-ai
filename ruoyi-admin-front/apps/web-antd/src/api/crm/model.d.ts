import type { BaseEntity } from '#/api/common';

export interface CrmCustomer extends BaseEntity {
  customerId: number;
  name: string;
  shortName?: string;
  code: string;
  type?: string;
  level?: string;
  industry?: string;
  province?: string;
  city?: string;
  district?: string;
  address?: string;
  website?: string;
  status?: string;
  scale?: string;
  remark?: string;
}

export interface CrmContact extends BaseEntity {
  contactId: number;
  customerId: number;
  name: string;
  phone?: string;
  email?: string;
  wechat?: string;
  position?: string;
  department?: string;
  decisionRole?: string;
  remark?: string;
}

export interface CrmOpportunity extends BaseEntity {
  opportunityId: number;
  customerId: number;
  contactId?: number;
  name: string;
  estimatedAmount?: number;
  estimatedCloseDate?: string;
  source?: string;
  stage?: string;
  successRate?: number;
  remark?: string;
}

export interface CrmFollowRecord extends BaseEntity {
  followId: number;
  opportunityId?: number;
  customerId: number;
  contactId?: number;
  followTime: string;
  followMethod?: string;
  content: string;
  result?: string;
  nextFollowTime?: string;
  remark?: string;
}

export interface CrmQuote extends BaseEntity {
  quoteId: number;
  opportunityId?: number;
  customerId: number;
  version?: number;
  totalAmount?: number;
  status?: string;
  remark?: string;
}

export interface CrmContract extends BaseEntity {
  contractId: number;
  opportunityId?: number;
  customerId: number;
  quoteId?: number;
  name: string;
  amount?: number;
  signedDate?: string;
  deliveryDate?: string;
  status?: string;
  remark?: string;
}

export interface CrmPaymentPlan extends BaseEntity {
  paymentId: number;
  contractId?: number;
  opportunityId?: number;
  customerId: number;
  stageName: string;
  amount?: number;
  plannedDate?: string;
  status?: string;
  remark?: string;
}

export type CrmEntity =
  | CrmContact
  | CrmContract
  | CrmCustomer
  | CrmFollowRecord
  | CrmOpportunity
  | CrmPaymentPlan
  | CrmQuote;
