import type { BaseEntity } from '#/api/common';

export interface MaterialPart extends BaseEntity {
  materialPartId: number;
  partCode?: string;
  partName?: string;
  specification?: string;
  unit?: string;
  material?: string;
  category?: string;
  defaultSupplierId?: number;
  status?: string;
  remark?: string;
}

export interface SrmSupplier extends BaseEntity {
  supplierId: number;
  supplierCode?: string;
  supplierName?: string;
  shortName?: string;
  contactName?: string;
  phone?: string;
  email?: string;
  address?: string;
  level?: string;
  status?: string;
  remark?: string;
}

export interface MesWorkOrder extends BaseEntity {
  workOrderId: number;
  contractId?: number;
  customerId?: number;
  opportunityId?: number;
  workOrderCode?: string;
  projectName?: string;
  productName?: string;
  quantity?: number;
  currentStage?: string;
  progress?: number;
  status?: string;
  planDeliveryDate?: string;
  actualDeliveryDate?: string;
  responsibleUserId?: number;
  remark?: string;
}

export interface EngineeringMaterial extends BaseEntity {
  engineeringMaterialId: number;
  workOrderId?: number;
  contractId?: number;
  partId?: number;
  partCode?: string;
  partName?: string;
  specification?: string;
  unit?: string;
  requiredQty?: number;
  stockQty?: number;
  shortageQty?: number;
  purchaseQty?: number;
  status?: string;
  remark?: string;
}

export interface WmsInventory extends BaseEntity {
  inventoryId: number;
  partId?: number;
  partCode?: string;
  partName?: string;
  specification?: string;
  unit?: string;
  stockQty?: number;
  availableQty?: number;
  lockedQty?: number;
  locationCode?: string;
  remark?: string;
}

export interface SrmPurchaseRequest extends BaseEntity {
  purchaseRequestId: number;
  workOrderId?: number;
  contractId?: number;
  engineeringMaterialId?: number;
  supplierId?: number;
  partId?: number;
  partCode?: string;
  partName?: string;
  specification?: string;
  unit?: string;
  requestQty?: number;
  status?: string;
  remark?: string;
}

export interface SrmPurchaseOrder extends BaseEntity {
  purchaseOrderId: number;
  purchaseOrderCode?: string;
  purchaseRequestId?: number;
  supplierId?: number;
  workOrderId?: number;
  contractId?: number;
  status?: string;
  orderDate?: string;
  expectedDeliveryDate?: string;
  remark?: string;
}

export interface SrmPurchaseOrderItem extends BaseEntity {
  purchaseOrderItemId: number;
  purchaseOrderId?: number;
  purchaseRequestId?: number;
  workOrderId?: number;
  contractId?: number;
  partId?: number;
  partCode?: string;
  partName?: string;
  specification?: string;
  unit?: string;
  purchaseQty?: number;
  price?: number;
  amount?: number;
  receivedQty?: number;
  status?: string;
  remark?: string;
}

export interface WmsReceiptOrder extends BaseEntity {
  receiptOrderId: number;
  purchaseOrderId?: number;
  supplierId?: number;
  workOrderId?: number;
  contractId?: number;
  receiptStatus?: string;
  receiptTime?: string;
  warehouseUserId?: number;
  remark?: string;
}

export interface WmsReceiptOrderItem extends BaseEntity {
  receiptOrderItemId: number;
  receiptOrderId?: number;
  purchaseOrderId?: number;
  purchaseOrderItemId?: number;
  workOrderId?: number;
  contractId?: number;
  partId?: number;
  partCode?: string;
  partName?: string;
  specification?: string;
  unit?: string;
  receiptQty?: number;
  status?: string;
  remark?: string;
}

export interface WmsIssueOrder extends BaseEntity {
  issueOrderId: number;
  workOrderId?: number;
  contractId?: number;
  issueStatus?: string;
  issueTime?: string;
  warehouseUserId?: number;
  remark?: string;
}

export interface WmsIssueOrderItem extends BaseEntity {
  issueOrderItemId: number;
  issueOrderId?: number;
  workOrderId?: number;
  contractId?: number;
  engineeringMaterialId?: number;
  partId?: number;
  partCode?: string;
  partName?: string;
  specification?: string;
  unit?: string;
  issueQty?: number;
  status?: string;
  remark?: string;
}

export interface MesWorkOrderStage extends BaseEntity {
  workOrderStageId: number;
  workOrderId?: number;
  stageCode?: string;
  stageName?: string;
  status?: string;
  responsibleUserId?: number;
  startTime?: string;
  endTime?: string;
  remark?: string;
}

export type XtpEntity =
  | MaterialPart
  | SrmSupplier
  | MesWorkOrder
  | EngineeringMaterial
  | WmsInventory
  | SrmPurchaseRequest
  | SrmPurchaseOrder
  | SrmPurchaseOrderItem
  | WmsReceiptOrder
  | WmsReceiptOrderItem
  | WmsIssueOrder
  | WmsIssueOrderItem
  | MesWorkOrderStage;
