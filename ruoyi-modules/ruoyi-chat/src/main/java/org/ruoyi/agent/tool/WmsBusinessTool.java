package org.ruoyi.agent.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.material.MaterialPart;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseOrder;
import org.ruoyi.system.domain.srm.SrmSupplier;
import org.ruoyi.system.domain.wms.WmsIssueOrder;
import org.ruoyi.system.domain.wms.WmsIssueOrderItem;
import org.ruoyi.system.domain.wms.WmsInventory;
import org.ruoyi.system.domain.wms.WmsReceiptOrder;
import org.ruoyi.system.domain.wms.WmsReceiptOrderItem;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.material.MaterialPartMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderMapper;
import org.ruoyi.system.mapper.srm.SrmSupplierMapper;
import org.ruoyi.system.mapper.wms.WmsIssueOrderItemMapper;
import org.ruoyi.system.mapper.wms.WmsIssueOrderMapper;
import org.ruoyi.system.mapper.wms.WmsInventoryMapper;
import org.ruoyi.system.mapper.wms.WmsReceiptOrderItemMapper;
import org.ruoyi.system.mapper.wms.WmsReceiptOrderMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WmsBusinessTool {

    private static final int MAX_ROWS = 100;

    private final CrmCustomerMapper customerMapper;
    private final CrmContractMapper contractMapper;
    private final MaterialPartMapper materialPartMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final SrmPurchaseOrderMapper purchaseOrderMapper;
    private final SrmSupplierMapper supplierMapper;
    private final SysUserMapper userMapper;
    private final WmsInventoryMapper inventoryMapper;
    private final WmsReceiptOrderMapper receiptOrderMapper;
    private final WmsReceiptOrderItemMapper receiptOrderItemMapper;
    private final WmsIssueOrderMapper issueOrderMapper;
    private final WmsIssueOrderItemMapper issueOrderItemMapper;

    @Tool("按物料编号查询 WMS 库存，并补齐物料名称、规格、单位等展示字段。优先使用 partCode。")
    public String wmsGetInventoryByPartCode(String partCode) {
        log.info("【WMS业务工具】wmsGetInventoryByPartCode partCode={}", partCode);
        if (StringUtils.isBlank(partCode)) {
            return missing("partCode", "请提供物料编号");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("criterion", Map.of("partCode", partCode));
        List<WmsInventory> inventory = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
            .eq(WmsInventory::getPartCode, partCode.trim())
            .orderByDesc(WmsInventory::getCreateTime)
            .last("limit " + MAX_ROWS));
        fillWarehouseDisplayFields(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), inventory);
        data.put("summary", Map.of("inventoryCount", inventory.size()));
        data.put("inventory", inventory);
        return success("wms_inventory", "查询成功", data);
    }

    @Tool("按物料ID查询 WMS 库存，并补齐物料名称、规格、单位等展示字段。")
    public String wmsGetInventoryByPartId(Long partId) {
        log.info("【WMS业务工具】wmsGetInventoryByPartId partId={}", partId);
        if (partId == null) {
            return missing("partId", "请提供物料ID");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("criterion", Map.of("partId", partId));
        List<WmsInventory> inventory = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
            .eq(WmsInventory::getPartId, partId)
            .orderByDesc(WmsInventory::getCreateTime)
            .last("limit " + MAX_ROWS));
        fillWarehouseDisplayFields(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), inventory);
        data.put("summary", Map.of("inventoryCount", inventory.size()));
        data.put("inventory", inventory);
        return success("wms_inventory", "查询成功", data);
    }

    @Tool("按物料编号、物料名称或物料ID查询 WMS 库存、收料明细和发料明细。用户问某物料库存/入库/出库时使用。")
    public String wmsGetPartWarehouseFlow(String partCodeOrNameOrId) {
        log.info("【WMS业务工具】wmsGetPartWarehouseFlow partCodeOrNameOrId={}", partCodeOrNameOrId);
        if (StringUtils.isBlank(partCodeOrNameOrId)) {
            return missing("part", "请提供物料编号、物料名称或物料ID");
        }
        List<MaterialPart> parts = findParts(partCodeOrNameOrId.trim());
        if (parts.isEmpty()) {
            return fail("wms_part_flow", "未找到物料", Map.of("part", partCodeOrNameOrId));
        }
        List<Long> partIds = parts.stream().map(MaterialPart::getPartId).filter(Objects::nonNull).collect(Collectors.toList());
        List<WmsInventory> inventory = inventoryByPartIds(partIds);
        List<WmsReceiptOrderItem> receiptItems = receiptOrderItemMapper.selectList(new LambdaQueryWrapper<WmsReceiptOrderItem>()
            .in(WmsReceiptOrderItem::getPartId, partIds)
            .orderByDesc(WmsReceiptOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<WmsIssueOrderItem> issueItems = issueOrderItemMapper.selectList(new LambdaQueryWrapper<WmsIssueOrderItem>()
            .in(WmsIssueOrderItem::getPartId, partIds)
            .orderByDesc(WmsIssueOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<Long> workOrderIds = partFlowWorkOrderIds(receiptItems, issueItems);
        List<MesWorkOrder> workOrders = workOrderIds.isEmpty() ? Collections.emptyList() : workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
            .in(MesWorkOrder::getWorkOrderId, workOrderIds)
            .orderByDesc(MesWorkOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        Map<String, Object> data = warehouseData(Map.of("part", partCodeOrNameOrId), workOrders, Collections.emptyList(), receiptItems, Collections.emptyList(), issueItems, inventory);
        data.put("parts", parts);
        return success("wms_part_flow", "查询成功", data);
    }

    @Tool("按工单编号查询 WMS 相关收料、发料、库存线索，返回合同、工单、物料、供应商和仓管人展示字段。")
    public String wmsGetWarehouseCluesByWorkOrderCode(String workOrderCode) {
        log.info("【WMS业务工具】wmsGetWarehouseCluesByWorkOrderCode workOrderCode={}", workOrderCode);
        if (StringUtils.isBlank(workOrderCode)) {
            return missing("workOrderCode", "请提供工单编号");
        }
        MesWorkOrder workOrder = workOrderMapper.selectOne(new LambdaQueryWrapper<MesWorkOrder>()
            .eq(MesWorkOrder::getWorkOrderCode, workOrderCode.trim())
            .last("limit 1"));
        if (workOrder == null) {
            return fail("wms_warehouse_clues", "未找到工单", Map.of("workOrderCode", workOrderCode));
        }
        return success("wms_warehouse_clues", "查询成功", warehouseCluesByWorkOrderIds(List.of(workOrder.getWorkOrderId()), List.of(workOrder), Map.of("workOrderCode", workOrderCode)));
    }

    @Tool("按工单ID查询 WMS 相关收料、发料、库存线索，返回合同、工单、物料、供应商和仓管人展示字段。")
    public String wmsGetWarehouseCluesByWorkOrderId(Long workOrderId) {
        log.info("【WMS业务工具】wmsGetWarehouseCluesByWorkOrderId workOrderId={}", workOrderId);
        if (workOrderId == null) {
            return missing("workOrderId", "请提供工单ID");
        }
        MesWorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            return fail("wms_warehouse_clues", "未找到工单", Map.of("workOrderId", workOrderId));
        }
        return success("wms_warehouse_clues", "查询成功", warehouseCluesByWorkOrderIds(List.of(workOrderId), List.of(workOrder), Map.of("workOrderId", workOrderId)));
    }

    @Tool("按合同ID查询 WMS 相关工单、收料、发料和库存线索，返回合同、工单、物料、供应商和仓管人展示字段。")
    public String wmsGetWarehouseCluesByContractId(Long contractId) {
        log.info("【WMS业务工具】wmsGetWarehouseCluesByContractId contractId={}", contractId);
        if (contractId == null) {
            return missing("contractId", "请提供合同ID");
        }
        List<MesWorkOrder> workOrders = workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
            .eq(MesWorkOrder::getContractId, contractId)
            .orderByDesc(MesWorkOrder::getCreateTime)
            .last("limit " + MAX_ROWS));

        List<WmsReceiptOrder> receiptOrders = receiptOrderMapper.selectList(new LambdaQueryWrapper<WmsReceiptOrder>()
            .eq(WmsReceiptOrder::getContractId, contractId)
            .orderByDesc(WmsReceiptOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<WmsReceiptOrderItem> receiptItems = receiptOrderItemMapper.selectList(new LambdaQueryWrapper<WmsReceiptOrderItem>()
            .eq(WmsReceiptOrderItem::getContractId, contractId)
            .orderByDesc(WmsReceiptOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<WmsIssueOrder> issueOrders = issueOrderMapper.selectList(new LambdaQueryWrapper<WmsIssueOrder>()
            .eq(WmsIssueOrder::getContractId, contractId)
            .orderByDesc(WmsIssueOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<WmsIssueOrderItem> issueItems = issueOrderItemMapper.selectList(new LambdaQueryWrapper<WmsIssueOrderItem>()
            .eq(WmsIssueOrderItem::getContractId, contractId)
            .orderByDesc(WmsIssueOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));
        Map<String, Object> data = warehouseData(Map.of("contractId", contractId), workOrders, receiptOrders, receiptItems, issueOrders, issueItems, inventoryByPartIds(partIds(receiptItems, issueItems)));
        return success("wms_warehouse_clues", "查询成功", data);
    }

    @Tool("按客户ID、客户编号或客户名称查询 WMS 相关工单、收料、发料和库存线索，返回合同、工单、物料、供应商和仓管人展示字段。")
    public String wmsGetWarehouseCluesByCustomer(String customerIdOrCodeOrName) {
        log.info("【WMS业务工具】wmsGetWarehouseCluesByCustomer customerIdOrCodeOrName={}", customerIdOrCodeOrName);
        if (StringUtils.isBlank(customerIdOrCodeOrName)) {
            return missing("customer", "请提供客户ID、客户编号或客户名称");
        }
        List<CrmCustomer> customers = findCustomers(customerIdOrCodeOrName.trim());
        if (customers.isEmpty()) {
            return fail("wms_warehouse_clues", "未找到客户", Map.of("customer", customerIdOrCodeOrName));
        }
        List<Long> customerIds = customers.stream().map(CrmCustomer::getCustomerId).collect(Collectors.toList());
        List<MesWorkOrder> workOrders = workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
            .in(MesWorkOrder::getCustomerId, customerIds)
            .orderByDesc(MesWorkOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<Long> workOrderIds = workOrders.stream().map(MesWorkOrder::getWorkOrderId).collect(Collectors.toList());
        Map<String, Object> data = warehouseCluesByWorkOrderIds(workOrderIds, workOrders, Map.of("customer", customerIdOrCodeOrName));
        data.put("customers", customers);
        return success("wms_warehouse_clues", "查询成功", data);
    }

    private Map<String, Object> warehouseCluesByWorkOrderIds(List<Long> workOrderIds, List<MesWorkOrder> workOrders, Map<String, ?> criterion) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return warehouseData(criterion, workOrders, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        List<WmsReceiptOrder> receiptOrders = receiptOrderMapper.selectList(new LambdaQueryWrapper<WmsReceiptOrder>()
            .in(WmsReceiptOrder::getWorkOrderId, workOrderIds)
            .orderByDesc(WmsReceiptOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<WmsReceiptOrderItem> receiptItems = receiptOrderItemMapper.selectList(new LambdaQueryWrapper<WmsReceiptOrderItem>()
            .in(WmsReceiptOrderItem::getWorkOrderId, workOrderIds)
            .orderByDesc(WmsReceiptOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<WmsIssueOrder> issueOrders = issueOrderMapper.selectList(new LambdaQueryWrapper<WmsIssueOrder>()
            .in(WmsIssueOrder::getWorkOrderId, workOrderIds)
            .orderByDesc(WmsIssueOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<WmsIssueOrderItem> issueItems = issueOrderItemMapper.selectList(new LambdaQueryWrapper<WmsIssueOrderItem>()
            .in(WmsIssueOrderItem::getWorkOrderId, workOrderIds)
            .orderByDesc(WmsIssueOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));

        return warehouseData(criterion, workOrders, receiptOrders, receiptItems, issueOrders, issueItems, inventoryByPartIds(partIds(receiptItems, issueItems)));
    }

    @Tool("按收料单ID查询 WMS 收料单、收料明细、关联工单和库存线索。")
    public String wmsGetReceiptOrderById(Long receiptOrderId) {
        log.info("【WMS业务工具】wmsGetReceiptOrderById receiptOrderId={}", receiptOrderId);
        if (receiptOrderId == null) {
            return missing("receiptOrderId", "请提供收料单ID");
        }
        WmsReceiptOrder receiptOrder = receiptOrderMapper.selectById(receiptOrderId);
        if (receiptOrder == null) {
            return fail("wms_receipt_order", "未找到收料单", Map.of("receiptOrderId", receiptOrderId));
        }
        List<WmsReceiptOrderItem> receiptItems = receiptOrderItemMapper.selectList(new LambdaQueryWrapper<WmsReceiptOrderItem>()
            .eq(WmsReceiptOrderItem::getReceiptOrderId, receiptOrderId)
            .orderByDesc(WmsReceiptOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<MesWorkOrder> workOrders = receiptOrder.getWorkOrderId() == null ? Collections.emptyList() : workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
            .eq(MesWorkOrder::getWorkOrderId, receiptOrder.getWorkOrderId())
            .last("limit 1"));
        return success("wms_receipt_order", "查询成功", warehouseData(Map.of("receiptOrderId", receiptOrderId), workOrders, List.of(receiptOrder), receiptItems, Collections.emptyList(), Collections.emptyList(), inventoryByPartIds(partIds(receiptItems, Collections.emptyList()))));
    }

    @Tool("按发料单ID查询 WMS 发料单、发料明细、关联工单和库存线索。")
    public String wmsGetIssueOrderById(Long issueOrderId) {
        log.info("【WMS业务工具】wmsGetIssueOrderById issueOrderId={}", issueOrderId);
        if (issueOrderId == null) {
            return missing("issueOrderId", "请提供发料单ID");
        }
        WmsIssueOrder issueOrder = issueOrderMapper.selectById(issueOrderId);
        if (issueOrder == null) {
            return fail("wms_issue_order", "未找到发料单", Map.of("issueOrderId", issueOrderId));
        }
        List<WmsIssueOrderItem> issueItems = issueOrderItemMapper.selectList(new LambdaQueryWrapper<WmsIssueOrderItem>()
            .eq(WmsIssueOrderItem::getIssueOrderId, issueOrderId)
            .orderByDesc(WmsIssueOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<MesWorkOrder> workOrders = issueOrder.getWorkOrderId() == null ? Collections.emptyList() : workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
            .eq(MesWorkOrder::getWorkOrderId, issueOrder.getWorkOrderId())
            .last("limit 1"));
        return success("wms_issue_order", "查询成功", warehouseData(Map.of("issueOrderId", issueOrderId), workOrders, Collections.emptyList(), Collections.emptyList(), List.of(issueOrder), issueItems, inventoryByPartIds(partIds(Collections.emptyList(), issueItems))));
    }

    private Map<String, Object> warehouseData(Map<String, ?> criterion, List<MesWorkOrder> workOrders,
                                              List<WmsReceiptOrder> receiptOrders, List<WmsReceiptOrderItem> receiptItems,
                                              List<WmsIssueOrder> issueOrders, List<WmsIssueOrderItem> issueItems,
                                              List<WmsInventory> inventory) {
        fillWarehouseDisplayFields(workOrders, receiptOrders, receiptItems, issueOrders, issueItems, inventory);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("criterion", criterion);
        data.put("summary", Map.of(
            "workOrderCount", workOrders.size(),
            "receiptOrderCount", receiptOrders.size(),
            "receiptItemCount", receiptItems.size(),
            "issueOrderCount", issueOrders.size(),
            "issueItemCount", issueItems.size(),
            "inventoryCount", inventory.size()
        ));
        data.put("workOrders", workOrders);
        data.put("receiptOrders", receiptOrders);
        data.put("receiptItems", receiptItems);
        data.put("issueOrders", issueOrders);
        data.put("issueItems", issueItems);
        data.put("inventory", inventory);
        return data;
    }

    private List<Long> partIds(List<WmsReceiptOrderItem> receiptItems, List<WmsIssueOrderItem> issueItems) {
        List<Long> receiptPartIds = receiptItems.stream()
            .map(WmsReceiptOrderItem::getPartId)
            .filter(partId -> partId != null)
            .collect(Collectors.toList());
        List<Long> issuePartIds = issueItems.stream()
            .map(WmsIssueOrderItem::getPartId)
            .filter(partId -> partId != null)
            .collect(Collectors.toList());
        receiptPartIds.addAll(issuePartIds);
        return receiptPartIds.stream().distinct().collect(Collectors.toList());
    }

    private List<WmsInventory> inventoryByPartIds(List<Long> partIds) {
        if (partIds == null || partIds.isEmpty()) {
            return Collections.emptyList();
        }
        return inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
            .in(WmsInventory::getPartId, partIds)
            .orderByDesc(WmsInventory::getCreateTime)
            .last("limit " + MAX_ROWS));
    }

    private List<Long> partFlowWorkOrderIds(List<WmsReceiptOrderItem> receiptItems, List<WmsIssueOrderItem> issueItems) {
        List<Long> ids = receiptItems.stream().map(WmsReceiptOrderItem::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toList());
        ids.addAll(issueItems.stream().map(WmsIssueOrderItem::getWorkOrderId).filter(Objects::nonNull).toList());
        return ids.stream().distinct().collect(Collectors.toList());
    }

    private void fillWarehouseDisplayFields(List<MesWorkOrder> workOrders, List<WmsReceiptOrder> receiptOrders,
                                            List<WmsReceiptOrderItem> receiptItems, List<WmsIssueOrder> issueOrders,
                                            List<WmsIssueOrderItem> issueItems, List<WmsInventory> inventory) {
        Set<Long> customerIds = workOrders.stream().map(MesWorkOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = workOrders.stream().map(MesWorkOrder::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        contractIds.addAll(receiptOrders.stream().map(WmsReceiptOrder::getContractId).filter(Objects::nonNull).collect(Collectors.toSet()));
        contractIds.addAll(receiptItems.stream().map(WmsReceiptOrderItem::getContractId).filter(Objects::nonNull).collect(Collectors.toSet()));
        contractIds.addAll(issueOrders.stream().map(WmsIssueOrder::getContractId).filter(Objects::nonNull).collect(Collectors.toSet()));
        contractIds.addAll(issueItems.stream().map(WmsIssueOrderItem::getContractId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<Long> workOrderIds = workOrders.stream().map(MesWorkOrder::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        workOrderIds.addAll(receiptOrders.stream().map(WmsReceiptOrder::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet()));
        workOrderIds.addAll(receiptItems.stream().map(WmsReceiptOrderItem::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet()));
        workOrderIds.addAll(issueOrders.stream().map(WmsIssueOrder::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet()));
        workOrderIds.addAll(issueItems.stream().map(WmsIssueOrderItem::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<Long> purchaseOrderIds = receiptOrders.stream().map(WmsReceiptOrder::getPurchaseOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        purchaseOrderIds.addAll(receiptItems.stream().map(WmsReceiptOrderItem::getPurchaseOrderId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<Long> supplierIds = receiptOrders.stream().map(WmsReceiptOrder::getSupplierId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> partIds = inventory.stream().map(WmsInventory::getPartId).filter(Objects::nonNull).collect(Collectors.toSet());
        partIds.addAll(receiptItems.stream().map(WmsReceiptOrderItem::getPartId).filter(Objects::nonNull).collect(Collectors.toSet()));
        partIds.addAll(issueItems.stream().map(WmsIssueOrderItem::getPartId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<Long> userIds = receiptOrders.stream().map(WmsReceiptOrder::getWarehouseUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        userIds.addAll(issueOrders.stream().map(WmsIssueOrder::getWarehouseUserId).filter(Objects::nonNull).collect(Collectors.toSet()));

        Map<Long, CrmCustomer> customers = customerIds.isEmpty() ? Map.of() : customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
            .in(CrmCustomer::getCustomerId, customerIds)).stream().collect(Collectors.toMap(CrmCustomer::getCustomerId, Function.identity(), (a, b) -> a));
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(new LambdaQueryWrapper<CrmContract>()
            .in(CrmContract::getContractId, contractIds)).stream().collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        Map<Long, MesWorkOrder> workOrderMap = workOrderIds.isEmpty() ? Map.of() : workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
            .in(MesWorkOrder::getWorkOrderId, workOrderIds)).stream().collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, SrmPurchaseOrder> purchaseOrders = purchaseOrderIds.isEmpty() ? Map.of() : purchaseOrderMapper.selectList(new LambdaQueryWrapper<SrmPurchaseOrder>()
            .in(SrmPurchaseOrder::getPurchaseOrderId, purchaseOrderIds)).stream().collect(Collectors.toMap(SrmPurchaseOrder::getPurchaseOrderId, Function.identity(), (a, b) -> a));
        Map<Long, SrmSupplier> suppliers = supplierIds.isEmpty() ? Map.of() : supplierMapper.selectList(new LambdaQueryWrapper<SrmSupplier>()
            .in(SrmSupplier::getSupplierId, supplierIds)).stream().collect(Collectors.toMap(SrmSupplier::getSupplierId, Function.identity(), (a, b) -> a));
        Map<Long, MaterialPart> parts = partIds.isEmpty() ? Map.of() : materialPartMapper.selectList(new LambdaQueryWrapper<MaterialPart>()
            .in(MaterialPart::getPartId, partIds)).stream().collect(Collectors.toMap(MaterialPart::getPartId, Function.identity(), (a, b) -> a));
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of() : userMapper.selectList(new LambdaQueryWrapper<SysUser>()
            .in(SysUser::getUserId, userIds)).stream().collect(Collectors.toMap(SysUser::getUserId, Function.identity(), (a, b) -> a));

        workOrders.forEach(workOrder -> fillWorkOrder(workOrder, customers, contracts));
        receiptOrders.forEach(order -> fillReceiptOrder(order, purchaseOrders, suppliers, workOrderMap, contracts, users));
        receiptItems.forEach(item -> fillReceiptItem(item, purchaseOrders, workOrderMap, contracts, parts));
        issueOrders.forEach(order -> fillIssueOrder(order, workOrderMap, contracts, users));
        issueItems.forEach(item -> fillIssueItem(item, workOrderMap, contracts, parts));
        inventory.forEach(item -> fillInventory(item, parts));
    }

    private void fillWorkOrder(MesWorkOrder workOrder, Map<Long, CrmCustomer> customers, Map<Long, CrmContract> contracts) {
        CrmCustomer customer = customers.get(workOrder.getCustomerId());
        if (customer != null) {
            workOrder.setCustomerCode(customer.getCode());
            workOrder.setCustomerName(customer.getName());
        }
        CrmContract contract = contracts.get(workOrder.getContractId());
        if (contract != null) {
            workOrder.setContractName(contract.getName());
        }
    }

    private void fillReceiptOrder(WmsReceiptOrder order, Map<Long, SrmPurchaseOrder> purchaseOrders,
                                  Map<Long, SrmSupplier> suppliers, Map<Long, MesWorkOrder> workOrders,
                                  Map<Long, CrmContract> contracts, Map<Long, SysUser> users) {
        SrmPurchaseOrder purchaseOrder = purchaseOrders.get(order.getPurchaseOrderId());
        if (purchaseOrder != null) {
            order.setPurchaseOrderCode(purchaseOrder.getPurchaseOrderCode());
        }
        SrmSupplier supplier = suppliers.get(order.getSupplierId());
        if (supplier != null) {
            order.setSupplierCode(supplier.getSupplierCode());
            order.setSupplierName(supplier.getSupplierName());
        }
        fillWorkOrderFields(order::setWorkOrderCode, order::setProjectName, workOrders.get(order.getWorkOrderId()));
        fillContractName(order::setContractName, contracts.get(order.getContractId()));
        order.setWarehouseUserName(userName(users.get(order.getWarehouseUserId())));
    }

    private void fillReceiptItem(WmsReceiptOrderItem item, Map<Long, SrmPurchaseOrder> purchaseOrders,
                                 Map<Long, MesWorkOrder> workOrders, Map<Long, CrmContract> contracts,
                                 Map<Long, MaterialPart> parts) {
        SrmPurchaseOrder purchaseOrder = purchaseOrders.get(item.getPurchaseOrderId());
        if (purchaseOrder != null) {
            item.setPurchaseOrderCode(purchaseOrder.getPurchaseOrderCode());
        }
        item.setReceiptOrderName("收料单 #" + item.getReceiptOrderId());
        fillWorkOrderFields(item::setWorkOrderCode, item::setProjectName, workOrders.get(item.getWorkOrderId()));
        fillContractName(item::setContractName, contracts.get(item.getContractId()));
        fillPartFields(item::setPartCode, item::setPartName, item::setSpecification, item::setUnit, parts.get(item.getPartId()));
    }

    private void fillIssueOrder(WmsIssueOrder order, Map<Long, MesWorkOrder> workOrders,
                                Map<Long, CrmContract> contracts, Map<Long, SysUser> users) {
        fillWorkOrderFields(order::setWorkOrderCode, order::setProjectName, workOrders.get(order.getWorkOrderId()));
        fillContractName(order::setContractName, contracts.get(order.getContractId()));
        order.setWarehouseUserName(userName(users.get(order.getWarehouseUserId())));
    }

    private void fillIssueItem(WmsIssueOrderItem item, Map<Long, MesWorkOrder> workOrders,
                               Map<Long, CrmContract> contracts, Map<Long, MaterialPart> parts) {
        item.setIssueOrderName("发料单 #" + item.getIssueOrderId());
        fillWorkOrderFields(item::setWorkOrderCode, item::setProjectName, workOrders.get(item.getWorkOrderId()));
        fillContractName(item::setContractName, contracts.get(item.getContractId()));
        fillPartFields(item::setPartCode, item::setPartName, item::setSpecification, item::setUnit, parts.get(item.getPartId()));
        item.setEngineeringMaterialName(List.of(
                Objects.toString(item.getPartCode(), ""),
                Objects.toString(item.getPartName(), "")
            ).stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" / ")));
    }

    private void fillInventory(WmsInventory inventory, Map<Long, MaterialPart> parts) {
        fillPartFields(inventory::setPartCode, inventory::setPartName, inventory::setSpecification, inventory::setUnit, parts.get(inventory.getPartId()));
    }

    private void fillWorkOrderFields(java.util.function.Consumer<String> codeSetter, java.util.function.Consumer<String> projectSetter, MesWorkOrder workOrder) {
        if (workOrder != null) {
            codeSetter.accept(workOrder.getWorkOrderCode());
            projectSetter.accept(workOrder.getProjectName());
        }
    }

    private void fillContractName(java.util.function.Consumer<String> setter, CrmContract contract) {
        if (contract != null) {
            setter.accept(contract.getName());
        }
    }

    private void fillPartFields(java.util.function.Consumer<String> codeSetter, java.util.function.Consumer<String> nameSetter,
                                java.util.function.Consumer<String> specSetter, java.util.function.Consumer<String> unitSetter,
                                MaterialPart part) {
        if (part != null) {
            codeSetter.accept(part.getPartCode());
            nameSetter.accept(part.getPartName());
            specSetter.accept(part.getSpecification());
            unitSetter.accept(part.getUnit());
        }
    }

    private List<CrmCustomer> findCustomers(String keyword) {
        Long customerId = tryParseLong(keyword);
        LambdaQueryWrapper<CrmCustomer> wrapper = new LambdaQueryWrapper<CrmCustomer>()
            .and(w -> {
                if (customerId != null) {
                    w.eq(CrmCustomer::getCustomerId, customerId).or();
                }
                w.eq(CrmCustomer::getCode, keyword)
                    .or()
                    .like(CrmCustomer::getName, keyword)
                    .or()
                    .like(CrmCustomer::getShortName, keyword);
            })
            .orderByDesc(CrmCustomer::getCreateTime)
            .last("limit " + MAX_ROWS);
        return customerMapper.selectList(wrapper);
    }

    private List<MaterialPart> findParts(String keyword) {
        Long partId = tryParseLong(keyword);
        LambdaQueryWrapper<MaterialPart> wrapper = new LambdaQueryWrapper<MaterialPart>()
            .and(w -> {
                if (partId != null) {
                    w.eq(MaterialPart::getPartId, partId).or();
                }
                w.eq(MaterialPart::getPartCode, keyword)
                    .or()
                    .like(MaterialPart::getPartName, keyword);
            })
            .orderByDesc(MaterialPart::getCreateTime)
            .last("limit " + MAX_ROWS);
        return materialPartMapper.selectList(wrapper);
    }

    private String userName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.isNotBlank(user.getNickName()) ? user.getNickName() : user.getUserName();
    }

    private Long tryParseLong(String value) {
        try {
            return StringUtils.isBlank(value) ? null : Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String success(String type, String message, Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("type", type);
        result.put("message", message);
        result.put("data", data);
        return JsonUtils.toJsonString(result);
    }

    private String fail(String type, String message, Map<String, ?> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("type", type);
        result.put("message", message);
        result.put("data", data);
        return JsonUtils.toJsonString(result);
    }

    private String missing(String need, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("type", "missing");
        result.put("message", message);
        result.put("need", need);
        return JsonUtils.toJsonString(result);
    }
}
