import type { BaseEntity } from '#/api/common';

export interface MaterialPart extends BaseEntity {
  partId: number;
  partCode?: string;
  partName?: string;
  specification?: string;
  unit?: string;
  material?: string;
  category?: string;
  defaultSupplierId?: number;
  defaultSupplierCode?: string;
  defaultSupplierName?: string;
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
  contractName?: string;
  customerId?: number;
  customerCode?: string;
  customerName?: string;
  opportunityId?: number;
  opportunityName?: string;
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
  responsibleUserName?: string;
  remark?: string;
}

export interface EngineeringMaterial extends BaseEntity {
  engineeringMaterialId: number;
  workOrderId?: number;
  workOrderCode?: string;
  projectName?: string;
  contractId?: number;
  contractName?: string;
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
  workOrderCode?: string;
  projectName?: string;
  contractId?: number;
  contractName?: string;
  engineeringMaterialId?: number;
  engineeringMaterialName?: string;
  supplierId?: number;
  supplierCode?: string;
  supplierName?: string;
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
  purchaseRequestName?: string;
  supplierId?: number;
  supplierCode?: string;
  supplierName?: string;
  workOrderId?: number;
  workOrderCode?: string;
  projectName?: string;
  contractId?: number;
  contractName?: string;
  status?: string;
  orderDate?: string;
  expectedDeliveryDate?: string;
  remark?: string;
}

export interface SrmPurchaseOrderItem extends BaseEntity {
  purchaseOrderItemId: number;
  purchaseOrderId?: number;
  purchaseOrderCode?: string;
  purchaseRequestId?: number;
  purchaseRequestName?: string;
  workOrderId?: number;
  workOrderCode?: string;
  projectName?: string;
  contractId?: number;
  contractName?: string;
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
  purchaseOrderCode?: string;
  supplierId?: number;
  supplierCode?: string;
  supplierName?: string;
  workOrderId?: number;
  workOrderCode?: string;
  projectName?: string;
  contractId?: number;
  contractName?: string;
  receiptStatus?: string;
  receiptTime?: string;
  warehouseUserId?: number;
  warehouseUserName?: string;
  remark?: string;
}

export interface WmsReceiptOrderItem extends BaseEntity {
  receiptOrderItemId: number;
  receiptOrderId?: number;
  receiptOrderName?: string;
  purchaseOrderId?: number;
  purchaseOrderCode?: string;
  purchaseOrderItemId?: number;
  purchaseOrderItemName?: string;
  workOrderId?: number;
  workOrderCode?: string;
  projectName?: string;
  contractId?: number;
  contractName?: string;
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
  workOrderCode?: string;
  projectName?: string;
  contractId?: number;
  contractName?: string;
  issueStatus?: string;
  issueTime?: string;
  warehouseUserId?: number;
  warehouseUserName?: string;
  remark?: string;
}

export interface WmsIssueOrderItem extends BaseEntity {
  issueOrderItemId: number;
  issueOrderId?: number;
  issueOrderName?: string;
  workOrderId?: number;
  workOrderCode?: string;
  projectName?: string;
  contractId?: number;
  contractName?: string;
  engineeringMaterialId?: number;
  engineeringMaterialName?: string;
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
  workOrderCode?: string;
  projectName?: string;
  stageCode?: string;
  stageName?: string;
  status?: string;
  responsibleUserId?: number;
  responsibleUserName?: string;
  startTime?: string;
  endTime?: string;
  remark?: string;
}

export type XtpEntity =
  | EngineeringMaterial
  | MaterialPart
  | MesWorkOrder
  | MesWorkOrderStage
  | SrmPurchaseOrder
  | SrmPurchaseOrderItem
  | SrmPurchaseRequest
  | SrmSupplier
  | WmsInventory
  | WmsIssueOrder
  | WmsIssueOrderItem
  | WmsReceiptOrder
  | WmsReceiptOrderItem;
