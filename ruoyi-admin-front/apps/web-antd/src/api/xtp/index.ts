import type { ID, IDS, PageQuery } from '#/api/common';

import type {
  MaterialPart,
  SrmSupplier,
  MesWorkOrder,
  EngineeringMaterial,
  WmsInventory,
  SrmPurchaseRequest,
  SrmPurchaseOrder,
  SrmPurchaseOrderItem,
  WmsReceiptOrder,
  WmsReceiptOrderItem,
  WmsIssueOrder,
  WmsIssueOrderItem,
  MesWorkOrderStage,
} from './model';

import { commonExport } from '#/api/helper';
import { requestClient } from '#/api/request';

function createXtpApi<T>(root: string) {
  return {
    add(data: Partial<T>) { return requestClient.postWithMsg<void>(root, data); },
    export(data: Partial<T>) { return commonExport(`${root}/export`, data); },
    info(id: ID) { return requestClient.get<T>(`${root}/${id}`); },
    list(params?: PageQuery) { return requestClient.get<T[]>(`${root}/list`, { params }); },
    options(params?: Partial<T>) { return requestClient.get<T[]>(`${root}/options`, { params }); },
    remove(ids: IDS) { return requestClient.deleteWithMsg<void>(`${root}/${ids}`); },
    update(data: Partial<T>) { return requestClient.putWithMsg<void>(root, data); },
  };
}

export const materialPartApi = createXtpApi<MaterialPart>('/material/part');
export const srmSupplierApi = createXtpApi<SrmSupplier>('/srm/supplier');
export const mesWorkOrderApi = createXtpApi<MesWorkOrder>('/mes/work-order');
export const engineeringMaterialApi = createXtpApi<EngineeringMaterial>('/engineering/material');
export const wmsInventoryApi = createXtpApi<WmsInventory>('/wms/inventory');
export const srmPurchaseRequestApi = createXtpApi<SrmPurchaseRequest>('/srm/purchase-request');
export const srmPurchaseOrderApi = createXtpApi<SrmPurchaseOrder>('/srm/purchase-order');
export const srmPurchaseOrderItemApi = createXtpApi<SrmPurchaseOrderItem>('/srm/purchase-order-item');
export const wmsReceiptOrderApi = createXtpApi<WmsReceiptOrder>('/wms/receipt-order');
export const wmsReceiptOrderItemApi = createXtpApi<WmsReceiptOrderItem>('/wms/receipt-order-item');
export const wmsIssueOrderApi = createXtpApi<WmsIssueOrder>('/wms/issue-order');
export const wmsIssueOrderItemApi = createXtpApi<WmsIssueOrderItem>('/wms/issue-order-item');
export const mesWorkOrderStageApi = createXtpApi<MesWorkOrderStage>('/mes/work-order-stage');

export const xtpFlowApi = {
  checkInventory(workOrderId: number | string) {
    return requestClient.post(`/xtp/flow/work-order/${workOrderId}/check-inventory`);
  },
  createIssueOrder(workOrderId: number | string, data: { warehouseUserId?: number }) {
    return requestClient.postWithMsg(`/xtp/flow/work-order/${workOrderId}/create-issue-order`, data);
  },
  createPurchaseOrder(purchaseRequestId: number | string, data: { expectedDeliveryDate?: string; price?: number }) {
    return requestClient.postWithMsg(`/xtp/flow/purchase-request/${purchaseRequestId}/create-purchase-order`, data);
  },
  createReceiptOrder(purchaseOrderId: number | string, data: { warehouseUserId?: number }) {
    return requestClient.postWithMsg(`/xtp/flow/purchase-order/${purchaseOrderId}/create-receipt-order`, data);
  },
  generatePurchaseRequests(workOrderId: number | string) {
    return requestClient.postWithMsg(`/xtp/flow/work-order/${workOrderId}/generate-purchase-requests`);
  },
  generateWorkOrder(contractId: number | string, data: { productName?: string; quantity?: number; responsibleUserId?: number }) {
    return requestClient.postWithMsg(`/xtp/flow/contract/${contractId}/generate-work-order`, data);
  },
  listStages(workOrderId: number | string) {
    return requestClient.get(`/xtp/flow/work-order/${workOrderId}/stages`);
  },
  updateStage(workOrderId: number | string, data: { remark?: string; stageCode: string; status: string }) {
    return requestClient.postWithMsg(`/xtp/flow/work-order/${workOrderId}/stage`, data);
  },
};

