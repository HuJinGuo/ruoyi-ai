package org.ruoyi.system.controller.xtp;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.system.domain.engineering.EngineeringMaterial;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.mes.MesWorkOrderStage;
import org.ruoyi.system.domain.srm.SrmPurchaseOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseRequest;
import org.ruoyi.system.domain.wms.WmsIssueOrder;
import org.ruoyi.system.domain.wms.WmsReceiptOrder;
import org.ruoyi.system.service.xtp.XtpManufacturingService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/xtp/flow")
public class XtpFlowController {

    private final XtpManufacturingService manufacturingService;

    @SaCheckPermission("crm:contract:edit")
    @PostMapping("/contract/{contractId}/generate-work-order")
    public R<MesWorkOrder> generateWorkOrder(@PathVariable Long contractId, @RequestBody GenerateWorkOrderRequest request) {
        return R.ok(manufacturingService.generateWorkOrder(
            contractId,
            request.getProductName(),
            request.getQuantity(),
            request.getResponsibleUserId()
        ));
    }

    @SaCheckPermission("engineering:material:edit")
    @PostMapping("/work-order/{workOrderId}/check-inventory")
    public R<List<EngineeringMaterial>> checkInventory(@PathVariable Long workOrderId) {
        return R.ok(manufacturingService.checkInventory(workOrderId));
    }

    @SaCheckPermission("srm:purchaseRequest:add")
    @PostMapping("/work-order/{workOrderId}/generate-purchase-requests")
    public R<List<SrmPurchaseRequest>> generatePurchaseRequests(@PathVariable Long workOrderId) {
        return R.ok(manufacturingService.generatePurchaseRequests(workOrderId));
    }

    @SaCheckPermission("srm:purchaseOrder:add")
    @PostMapping("/purchase-request/{purchaseRequestId}/create-purchase-order")
    public R<SrmPurchaseOrder> createPurchaseOrder(@PathVariable Long purchaseRequestId, @RequestBody CreatePurchaseOrderRequest request) {
        return R.ok(manufacturingService.createPurchaseOrder(
            purchaseRequestId,
            request.getPrice(),
            request.getExpectedDeliveryDate()
        ));
    }

    @SaCheckPermission("wms:receiptOrder:add")
    @PostMapping("/purchase-order/{purchaseOrderId}/create-receipt-order")
    public R<WmsReceiptOrder> createReceiptOrder(@PathVariable Long purchaseOrderId, @RequestBody WarehouseUserRequest request) {
        return R.ok(manufacturingService.createReceiptOrder(purchaseOrderId, request.getWarehouseUserId()));
    }

    @SaCheckPermission("wms:issueOrder:add")
    @PostMapping("/work-order/{workOrderId}/create-issue-order")
    public R<WmsIssueOrder> createIssueOrder(@PathVariable Long workOrderId, @RequestBody WarehouseUserRequest request) {
        return R.ok(manufacturingService.createIssueOrder(workOrderId, request.getWarehouseUserId()));
    }

    @SaCheckPermission("mes:workOrderStage:edit")
    @PostMapping("/work-order/{workOrderId}/stage")
    public R<MesWorkOrderStage> updateStage(@PathVariable Long workOrderId, @RequestBody UpdateStageRequest request) {
        return R.ok(manufacturingService.updateStage(
            workOrderId,
            request.getStageCode(),
            request.getStatus(),
            request.getRemark()
        ));
    }

    @SaCheckPermission("mes:workOrder:query")
    @GetMapping("/work-order/{workOrderId}/stages")
    public R<List<MesWorkOrderStage>> listStages(@PathVariable Long workOrderId) {
        return R.ok(manufacturingService.listStages(workOrderId));
    }

    @Data
    public static class GenerateWorkOrderRequest {
        private String productName;
        private Integer quantity;
        private Long responsibleUserId;
    }

    @Data
    public static class CreatePurchaseOrderRequest {
        private BigDecimal price;
        private Date expectedDeliveryDate;
    }

    @Data
    public static class WarehouseUserRequest {
        private Long warehouseUserId;
    }

    @Data
    public static class UpdateStageRequest {
        private String stageCode;
        private String status;
        private String remark;
    }
}
