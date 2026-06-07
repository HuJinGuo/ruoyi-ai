import type { ID, IDS, PageQuery } from '#/api/common';

import type {
  CrmContact,
  CrmContract,
  CrmCustomer,
  CrmFollowRecord,
  CrmOpportunity,
  CrmPaymentPlan,
  CrmQuote,
} from './model';

import { commonExport } from '#/api/helper';
import { requestClient } from '#/api/request';

function createCrmApi<T>(root: string) {
  return {
    add(data: Partial<T>) {
      return requestClient.postWithMsg<void>(root, data);
    },
    export(data: Partial<T>) {
      return commonExport(`${root}/export`, data);
    },
    info(id: ID) {
      return requestClient.get<T>(`${root}/${id}`);
    },
    list(params?: PageQuery) {
      return requestClient.get<T[]>(`${root}/list`, { params });
    },
    options(params?: Partial<T>) {
      return requestClient.get<T[]>(`${root}/options`, { params });
    },
    remove(ids: IDS) {
      return requestClient.deleteWithMsg<void>(`${root}/${ids}`);
    },
    update(data: Partial<T>) {
      return requestClient.putWithMsg<void>(root, data);
    },
  };
}

export const crmCustomerApi = createCrmApi<CrmCustomer>('/crm/customer');
export const crmContactApi = createCrmApi<CrmContact>('/crm/contact');
export const crmOpportunityApi =
  createCrmApi<CrmOpportunity>('/crm/opportunity');
export const crmFollowRecordApi =
  createCrmApi<CrmFollowRecord>('/crm/follow-record');
export const crmQuoteApi = createCrmApi<CrmQuote>('/crm/quote');
export const crmContractApi = createCrmApi<CrmContract>('/crm/contract');
export const crmPaymentPlanApi =
  createCrmApi<CrmPaymentPlan>('/crm/payment-plan');
