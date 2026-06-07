package org.ruoyi.system.service.xtp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.engineering.EngineeringMaterial;
import org.ruoyi.system.domain.material.MaterialPart;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.mes.MesWorkOrderStage;
import org.ruoyi.system.domain.srm.SrmPurchaseOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseOrderItem;
import org.ruoyi.system.domain.srm.SrmPurchaseRequest;
import org.ruoyi.system.domain.wms.WmsInventory;
import org.ruoyi.system.domain.wms.WmsIssueOrder;
import org.ruoyi.system.domain.wms.WmsIssueOrderItem;
import org.ruoyi.system.domain.wms.WmsReceiptOrder;
import org.ruoyi.system.domain.wms.WmsReceiptOrderItem;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.engineering.EngineeringMaterialMapper;
import org.ruoyi.system.mapper.material.MaterialPartMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderStageMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderItemMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseRequestMapper;
import org.ruoyi.system.mapper.wms.WmsInventoryMapper;
import org.ruoyi.system.mapper.wms.WmsIssueOrderItemMapper;
import org.ruoyi.system.mapper.wms.WmsIssueOrderMapper;
import org.ruoyi.system.mapper.wms.WmsReceiptOrderItemMapper;
import org.ruoyi.system.mapper.wms.WmsReceiptOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * XTP V1 合同驱动制造闭环服务。
 */
@Service
@RequiredArgsConstructor
public class XtpManufacturingService {

    private static final String STAGE_ENGINEERING = "ENGINEERING";
    private static final String STAGE_PURCHASE = "PURCHASE";
    private static final String STAGE_RECEIPT = "RECEIPT";
    private static final String STAGE_ISSUE = "ISSUE";
    private static final String STAGE_ASSEMBLY = "ASSEMBLY";
    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CrmContractMapper contractMapper;
    private final MaterialPartMapper partMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final MesWorkOrderStageMapper stageMapper;
    private final EngineeringMaterialMapper engineeringMaterialMapper;
    private final WmsInventoryMapper inventoryMapper;
    private final SrmPurchaseRequestMapper purchaseRequestMapper;
    private final SrmPurchaseOrderMapper purchaseOrderMapper;
    private final SrmPurchaseOrderItemMapper purchaseOrderItemMapper;
    private final WmsReceiptOrderMapper receiptOrderMapper;
    private final WmsReceiptOrderItemMapper receiptOrderItemMapper;
    private final WmsIssueOrderMapper issueOrderMapper;
    private final WmsIssueOrderItemMapper issueOrderItemMapper;

    @Transactional(rollbackFor = Exception.class)
    public MesWorkOrder generateWorkOrder(Long contractId, String productName, Integer quantity, Long responsibleUserId) {
        CrmContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new ServiceException("合同不存在");
        }
        MesWorkOrder existing = workOrderMapper.selectOne(new LambdaQueryWrapper<MesWorkOrder>()
            .eq(MesWorkOrder::getContractId, contractId)
            .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        MesWorkOrder workOrder = new MesWorkOrder();
        workOrder.setContractId(contract.getContractId());
        workOrder.setCustomerId(contract.getCustomerId());
        workOrder.setOpportunityId(contract.getOpportunityId());
        workOrder.setWorkOrderCode("WO" + LocalDateTime.now().format(CODE_TIME));
        workOrder.setProjectName(contract.getName());
        workOrder.setProductName(productName == null || productName.isBlank() ? contract.getName() : productName);
        workOrder.setQuantity(quantity == null || quantity < 1 ? 1 : quantity);
        workOrder.setCurrentStage(STAGE_ENGINEERING);
        workOrder.setProgress(BigDecimal.ZERO);
        workOrder.setStatus("pending");
        workOrder.setPlanDeliveryDate(contract.getDeliveryDate());
        workOrder.setResponsibleUserId(responsibleUserId);
        workOrderMapper.insert(workOrder);
        initStages(workOrder.getWorkOrderId(), responsibleUserId);
        contract.setStatus("executing");
        contractMapper.updateById(contract);
        return workOrder;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<EngineeringMaterial> checkInventory(Long workOrderId) {
        MesWorkOrder workOrder = requireWorkOrder(workOrderId);
        List<EngineeringMaterial> materials = engineeringMaterialMapper.selectList(new LambdaQueryWrapper<EngineeringMaterial>()
            .eq(EngineeringMaterial::getWorkOrderId, workOrderId));
        for (EngineeringMaterial material : materials) {
            BigDecimal availableQty = sumAvailableQty(material.getPartId());
            BigDecimal requiredQty = nvl(material.getRequiredQty());
            BigDecimal shortageQty = requiredQty.subtract(availableQty).max(BigDecimal.ZERO);
            material.setContractId(workOrder.getContractId());
            material.setStockQty(availableQty);
            material.setShortageQty(shortageQty);
            material.setPurchaseQty(shortageQty);
            if (shortageQty.compareTo(BigDecimal.ZERO) == 0) {
                material.setStatus("stock_enough");
            } else if (availableQty.compareTo(BigDecimal.ZERO) > 0) {
                material.setStatus("partial_shortage");
            } else {
                material.setStatus("full_shortage");
            }
            engineeringMaterialMapper.updateById(material);
        }
        finishStage(workOrderId, STAGE_ENGINEERING, STAGE_PURCHASE);
        return materials;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<SrmPurchaseRequest> generatePurchaseRequests(Long workOrderId) {
        MesWorkOrder workOrder = requireWorkOrder(workOrderId);
        List<EngineeringMaterial> shortages = engineeringMaterialMapper.selectList(new LambdaQueryWrapper<EngineeringMaterial>()
            .eq(EngineeringMaterial::getWorkOrderId, workOrderId)
            .gt(EngineeringMaterial::getShortageQty, BigDecimal.ZERO));
        for (EngineeringMaterial material : shortages) {
            boolean exists = purchaseRequestMapper.exists(new LambdaQueryWrapper<SrmPurchaseRequest>()
                .eq(SrmPurchaseRequest::getEngineeringMaterialId, material.getEngineeringMaterialId()));
            if (exists) {
                continue;
            }
            MaterialPart part = material.getPartId() == null ? null : partMapper.selectById(material.getPartId());
            SrmPurchaseRequest request = new SrmPurchaseRequest();
            request.setWorkOrderId(workOrderId);
            request.setContractId(workOrder.getContractId());
            request.setEngineeringMaterialId(material.getEngineeringMaterialId());
            request.setSupplierId(part == null ? null : part.getDefaultSupplierId());
            request.setPartId(material.getPartId());
            request.setPartCode(material.getPartCode());
            request.setPartName(material.getPartName());
            request.setSpecification(material.getSpecification());
            request.setUnit(material.getUnit());
            request.setRequestQty(nvl(material.getPurchaseQty()));
            request.setStatus("pending");
            purchaseRequestMapper.insert(request);
            material.setStatus("purchased");
            engineeringMaterialMapper.updateById(material);
        }
        startStage(workOrderId, STAGE_PURCHASE);
        return purchaseRequestMapper.selectList(new LambdaQueryWrapper<SrmPurchaseRequest>()
            .eq(SrmPurchaseRequest::getWorkOrderId, workOrderId)
            .orderByDesc(SrmPurchaseRequest::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public SrmPurchaseOrder createPurchaseOrder(Long purchaseRequestId, BigDecimal price, Date expectedDeliveryDate) {
        SrmPurchaseRequest request = purchaseRequestMapper.selectById(purchaseRequestId);
        if (request == null) {
            throw new ServiceException("采购需求不存在");
        }
        SrmPurchaseOrder existing = purchaseOrderMapper.selectOne(new LambdaQueryWrapper<SrmPurchaseOrder>()
            .eq(SrmPurchaseOrder::getPurchaseRequestId, purchaseRequestId)
            .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        SrmPurchaseOrder order = new SrmPurchaseOrder();
        order.setPurchaseOrderCode("PO" + LocalDateTime.now().format(CODE_TIME));
        order.setPurchaseRequestId(purchaseRequestId);
        order.setSupplierId(request.getSupplierId());
        order.setWorkOrderId(request.getWorkOrderId());
        order.setContractId(request.getContractId());
        order.setStatus("ordered");
        order.setOrderDate(new Date());
        order.setExpectedDeliveryDate(expectedDeliveryDate);
        purchaseOrderMapper.insert(order);

        BigDecimal qty = nvl(request.getRequestQty());
        BigDecimal itemPrice = nvl(price);
        SrmPurchaseOrderItem item = new SrmPurchaseOrderItem();
        item.setPurchaseOrderId(order.getPurchaseOrderId());
        item.setPurchaseRequestId(purchaseRequestId);
        item.setWorkOrderId(request.getWorkOrderId());
        item.setContractId(request.getContractId());
        item.setPartId(request.getPartId());
        item.setPartCode(request.getPartCode());
        item.setPartName(request.getPartName());
        item.setSpecification(request.getSpecification());
        item.setUnit(request.getUnit());
        item.setPurchaseQty(qty);
        item.setPrice(itemPrice);
        item.setAmount(qty.multiply(itemPrice).setScale(2, RoundingMode.HALF_UP));
        item.setReceivedQty(BigDecimal.ZERO);
        item.setStatus("purchasing");
        purchaseOrderItemMapper.insert(item);

        request.setStatus("purchasing");
        purchaseRequestMapper.updateById(request);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsReceiptOrder createReceiptOrder(Long purchaseOrderId, Long warehouseUserId) {
        SrmPurchaseOrder order = purchaseOrderMapper.selectById(purchaseOrderId);
        if (order == null) {
            throw new ServiceException("采购订单不存在");
        }
        WmsReceiptOrder receipt = new WmsReceiptOrder();
        receipt.setPurchaseOrderId(purchaseOrderId);
        receipt.setSupplierId(order.getSupplierId());
        receipt.setWorkOrderId(order.getWorkOrderId());
        receipt.setContractId(order.getContractId());
        receipt.setReceiptStatus("completed");
        receipt.setReceiptTime(new Date());
        receipt.setWarehouseUserId(warehouseUserId);
        receiptOrderMapper.insert(receipt);

        List<SrmPurchaseOrderItem> items = purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<SrmPurchaseOrderItem>()
            .eq(SrmPurchaseOrderItem::getPurchaseOrderId, purchaseOrderId));
        for (SrmPurchaseOrderItem item : items) {
            BigDecimal qty = nvl(item.getPurchaseQty()).subtract(nvl(item.getReceivedQty())).max(BigDecimal.ZERO);
            if (qty.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            WmsReceiptOrderItem receiptItem = new WmsReceiptOrderItem();
            receiptItem.setReceiptOrderId(receipt.getReceiptOrderId());
            receiptItem.setPurchaseOrderId(purchaseOrderId);
            receiptItem.setPurchaseOrderItemId(item.getPurchaseOrderItemId());
            receiptItem.setWorkOrderId(item.getWorkOrderId());
            receiptItem.setContractId(item.getContractId());
            receiptItem.setPartId(item.getPartId());
            receiptItem.setPartCode(item.getPartCode());
            receiptItem.setPartName(item.getPartName());
            receiptItem.setSpecification(item.getSpecification());
            receiptItem.setUnit(item.getUnit());
            receiptItem.setReceiptQty(qty);
            receiptItem.setStatus("completed");
            receiptOrderItemMapper.insert(receiptItem);
            increaseInventory(item, qty);
            item.setReceivedQty(nvl(item.getReceivedQty()).add(qty));
            item.setStatus("arrived_all");
            purchaseOrderItemMapper.updateById(item);
        }
        order.setStatus("arrived_all");
        purchaseOrderMapper.updateById(order);
        finishStage(order.getWorkOrderId(), STAGE_PURCHASE, STAGE_RECEIPT);
        finishStage(order.getWorkOrderId(), STAGE_RECEIPT, STAGE_ISSUE);
        return receipt;
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsIssueOrder createIssueOrder(Long workOrderId, Long warehouseUserId) {
        MesWorkOrder workOrder = requireWorkOrder(workOrderId);
        WmsIssueOrder issueOrder = new WmsIssueOrder();
        issueOrder.setWorkOrderId(workOrderId);
        issueOrder.setContractId(workOrder.getContractId());
        issueOrder.setIssueStatus("completed");
        issueOrder.setIssueTime(new Date());
        issueOrder.setWarehouseUserId(warehouseUserId);
        issueOrderMapper.insert(issueOrder);

        List<EngineeringMaterial> materials = engineeringMaterialMapper.selectList(new LambdaQueryWrapper<EngineeringMaterial>()
            .eq(EngineeringMaterial::getWorkOrderId, workOrderId));
        for (EngineeringMaterial material : materials) {
            BigDecimal qty = nvl(material.getRequiredQty());
            if (qty.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            WmsIssueOrderItem item = new WmsIssueOrderItem();
            item.setIssueOrderId(issueOrder.getIssueOrderId());
            item.setWorkOrderId(workOrderId);
            item.setContractId(workOrder.getContractId());
            item.setEngineeringMaterialId(material.getEngineeringMaterialId());
            item.setPartId(material.getPartId());
            item.setPartCode(material.getPartCode());
            item.setPartName(material.getPartName());
            item.setSpecification(material.getSpecification());
            item.setUnit(material.getUnit());
            item.setIssueQty(qty);
            item.setStatus("completed");
            issueOrderItemMapper.insert(item);
            decreaseInventory(material, qty);
            material.setStatus("issued");
            engineeringMaterialMapper.updateById(material);
        }
        finishStage(workOrderId, STAGE_ISSUE, STAGE_ASSEMBLY);
        updateWorkOrderProgress(workOrderId);
        return issueOrder;
    }

    @Transactional(rollbackFor = Exception.class)
    public MesWorkOrderStage updateStage(Long workOrderId, String stageCode, String status, String remark) {
        MesWorkOrderStage stage = stageMapper.selectOne(new LambdaQueryWrapper<MesWorkOrderStage>()
            .eq(MesWorkOrderStage::getWorkOrderId, workOrderId)
            .eq(MesWorkOrderStage::getStageCode, stageCode)
            .last("limit 1"));
        if (stage == null) {
            throw new ServiceException("工单阶段不存在");
        }
        stage.setStatus(status);
        stage.setRemark(remark);
        Date now = new Date();
        if ("PROCESSING".equals(status) && stage.getStartTime() == null) {
            stage.setStartTime(now);
        }
        if ("FINISHED".equals(status)) {
            stage.setEndTime(now);
        }
        stageMapper.updateById(stage);
        updateWorkOrderProgress(workOrderId);
        return stage;
    }

    public List<MesWorkOrderStage> listStages(Long workOrderId) {
        return stageMapper.selectList(new LambdaQueryWrapper<MesWorkOrderStage>()
            .eq(MesWorkOrderStage::getWorkOrderId, workOrderId)
            .orderByAsc(MesWorkOrderStage::getCreateTime));
    }

    private void initStages(Long workOrderId, Long responsibleUserId) {
        addStage(workOrderId, STAGE_ENGINEERING, "工程清算", "PROCESSING", responsibleUserId);
        addStage(workOrderId, STAGE_PURCHASE, "采购", "WAIT", responsibleUserId);
        addStage(workOrderId, STAGE_RECEIPT, "收料", "WAIT", responsibleUserId);
        addStage(workOrderId, STAGE_ISSUE, "发料", "WAIT", responsibleUserId);
        addStage(workOrderId, STAGE_ASSEMBLY, "分装", "WAIT", responsibleUserId);
        addStage(workOrderId, "FINAL_ASSEMBLY", "总装", "WAIT", responsibleUserId);
        addStage(workOrderId, "TEST", "调试", "WAIT", responsibleUserId);
        addStage(workOrderId, "DELIVERY", "发货", "WAIT", responsibleUserId);
        addStage(workOrderId, "SERVICE", "售后", "WAIT", responsibleUserId);
        addStage(workOrderId, "ACCEPTANCE", "验收", "WAIT", responsibleUserId);
    }

    private void addStage(Long workOrderId, String code, String name, String status, Long responsibleUserId) {
        MesWorkOrderStage stage = new MesWorkOrderStage();
        stage.setWorkOrderId(workOrderId);
        stage.setStageCode(code);
        stage.setStageName(name);
        stage.setStatus(status);
        stage.setResponsibleUserId(responsibleUserId);
        if ("PROCESSING".equals(status)) {
            stage.setStartTime(new Date());
        }
        stageMapper.insert(stage);
    }

    private void finishStage(Long workOrderId, String finishedStage, String nextStage) {
        updateStageIfExists(workOrderId, finishedStage, "FINISHED");
        updateStageIfExists(workOrderId, nextStage, "PROCESSING");
        updateWorkOrderProgress(workOrderId);
    }

    private void startStage(Long workOrderId, String stageCode) {
        updateStageIfExists(workOrderId, stageCode, "PROCESSING");
    }

    private void updateStageIfExists(Long workOrderId, String stageCode, String status) {
        MesWorkOrderStage stage = stageMapper.selectOne(new LambdaQueryWrapper<MesWorkOrderStage>()
            .eq(MesWorkOrderStage::getWorkOrderId, workOrderId)
            .eq(MesWorkOrderStage::getStageCode, stageCode)
            .last("limit 1"));
        if (stage == null) {
            return;
        }
        stage.setStatus(status);
        Date now = new Date();
        if ("PROCESSING".equals(status) && stage.getStartTime() == null) {
            stage.setStartTime(now);
        }
        if ("FINISHED".equals(status)) {
            stage.setEndTime(now);
        }
        stageMapper.updateById(stage);
    }

    private void updateWorkOrderProgress(Long workOrderId) {
        List<MesWorkOrderStage> stages = listStages(workOrderId);
        long finished = stages.stream().filter(stage -> "FINISHED".equals(stage.getStatus())).count();
        MesWorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null || stages.isEmpty()) {
            return;
        }
        BigDecimal progress = BigDecimal.valueOf(finished)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(stages.size()), 2, RoundingMode.HALF_UP);
        workOrder.setProgress(progress);
        stages.stream()
            .filter(stage -> "PROCESSING".equals(stage.getStatus()))
            .findFirst()
            .ifPresent(stage -> workOrder.setCurrentStage(stage.getStageCode()));
        if (progress.compareTo(BigDecimal.valueOf(100)) >= 0) {
            workOrder.setStatus("completed");
            workOrder.setActualDeliveryDate(new Date());
            CrmContract contract = contractMapper.selectById(workOrder.getContractId());
            if (contract != null) {
                contract.setStatus("completed");
                contractMapper.updateById(contract);
            }
        } else if (!"completed".equals(workOrder.getStatus())) {
            workOrder.setStatus("processing");
        }
        workOrderMapper.updateById(workOrder);
    }

    private MesWorkOrder requireWorkOrder(Long workOrderId) {
        MesWorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new ServiceException("工单不存在");
        }
        return workOrder;
    }

    private BigDecimal sumAvailableQty(Long partId) {
        if (partId == null) {
            return BigDecimal.ZERO;
        }
        return inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getPartId, partId))
            .stream()
            .map(item -> nvl(item.getAvailableQty()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void increaseInventory(SrmPurchaseOrderItem item, BigDecimal qty) {
        WmsInventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<WmsInventory>()
            .eq(WmsInventory::getPartId, item.getPartId())
            .last("limit 1"));
        if (inventory == null) {
            inventory = new WmsInventory();
            inventory.setPartId(item.getPartId());
            inventory.setPartCode(item.getPartCode());
            inventory.setPartName(item.getPartName());
            inventory.setSpecification(item.getSpecification());
            inventory.setUnit(item.getUnit());
            inventory.setStockQty(qty);
            inventory.setAvailableQty(qty);
            inventory.setLockedQty(BigDecimal.ZERO);
            inventoryMapper.insert(inventory);
            return;
        }
        inventory.setStockQty(nvl(inventory.getStockQty()).add(qty));
        inventory.setAvailableQty(nvl(inventory.getAvailableQty()).add(qty));
        inventoryMapper.updateById(inventory);
    }

    private void decreaseInventory(EngineeringMaterial material, BigDecimal qty) {
        WmsInventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<WmsInventory>()
            .eq(WmsInventory::getPartId, material.getPartId())
            .last("limit 1"));
        if (inventory == null) {
            throw new ServiceException("物料库存不存在：" + material.getPartName());
        }
        if (nvl(inventory.getAvailableQty()).compareTo(qty) < 0) {
            throw new ServiceException("可用库存不足：" + material.getPartName());
        }
        inventory.setAvailableQty(nvl(inventory.getAvailableQty()).subtract(qty));
        inventory.setStockQty(nvl(inventory.getStockQty()).subtract(qty));
        inventoryMapper.updateById(inventory);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
