package org.ruoyi.agent.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseOrderItem;
import org.ruoyi.system.domain.srm.SrmPurchaseRequest;
import org.ruoyi.system.domain.srm.SrmSupplier;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderItemMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseRequestMapper;
import org.ruoyi.system.mapper.srm.SrmSupplierMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class SrmBusinessTool {

    private static final int MAX_ROWS = 100;

    private final CrmCustomerMapper customerMapper;
    private final CrmContractMapper contractMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final SrmPurchaseRequestMapper purchaseRequestMapper;
    private final SrmPurchaseOrderMapper purchaseOrderMapper;
    private final SrmPurchaseOrderItemMapper purchaseOrderItemMapper;
    private final SrmSupplierMapper supplierMapper;

    @Tool("按工单编号查询 SRM 采购需求、采购订单和采购明细，返回供应商、合同、工单等展示字段。")
    public String srmGetProcurementByWorkOrderCode(String workOrderCode) {
        log.info("【SRM业务工具】srmGetProcurementByWorkOrderCode workOrderCode={}", workOrderCode);
        if (StringUtils.isBlank(workOrderCode)) {
            return missing("workOrderCode", "请提供工单编号");
        }
        MesWorkOrder workOrder = workOrderMapper.selectOne(new LambdaQueryWrapper<MesWorkOrder>()
            .eq(MesWorkOrder::getWorkOrderCode, workOrderCode.trim())
            .last("limit 1"));
        if (workOrder == null) {
            return fail("srm_procurement_status", "未找到工单", Map.of("workOrderCode", workOrderCode));
        }
        Map<String, Object> data = procurementByWorkOrderIds(List.of(workOrder.getWorkOrderId()), List.of(workOrder), Map.of("workOrderCode", workOrderCode));
        return success("srm_procurement_status", "查询成功", data);
    }

    @Tool("按工单ID查询 SRM 采购需求、采购订单和采购明细，返回供应商、合同、工单等展示字段。")
    public String srmGetProcurementByWorkOrderId(Long workOrderId) {
        log.info("【SRM业务工具】srmGetProcurementByWorkOrderId workOrderId={}", workOrderId);
        if (workOrderId == null) {
            return missing("workOrderId", "请提供工单ID");
        }
        MesWorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            return fail("srm_procurement_status", "未找到工单", Map.of("workOrderId", workOrderId));
        }
        Map<String, Object> data = procurementByWorkOrderIds(List.of(workOrderId), List.of(workOrder), Map.of("workOrderId", workOrderId));
        return success("srm_procurement_status", "查询成功", data);
    }

    @Tool("按合同ID查询相关工单、SRM 采购需求、采购订单和采购明细，返回供应商、合同、工单等展示字段。")
    public String srmGetProcurementByContractId(Long contractId) {
        log.info("【SRM业务工具】srmGetProcurementByContractId contractId={}", contractId);
        if (contractId == null) {
            return missing("contractId", "请提供合同ID");
        }
        List<MesWorkOrder> workOrders = workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
            .eq(MesWorkOrder::getContractId, contractId)
            .orderByDesc(MesWorkOrder::getCreateTime)
            .last("limit " + MAX_ROWS));

        List<SrmPurchaseRequest> purchaseRequests = purchaseRequestMapper.selectList(new LambdaQueryWrapper<SrmPurchaseRequest>()
            .eq(SrmPurchaseRequest::getContractId, contractId)
            .orderByDesc(SrmPurchaseRequest::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<SrmPurchaseOrder> purchaseOrders = purchaseOrderMapper.selectList(new LambdaQueryWrapper<SrmPurchaseOrder>()
            .eq(SrmPurchaseOrder::getContractId, contractId)
            .orderByDesc(SrmPurchaseOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<SrmPurchaseOrderItem> purchaseOrderItems = purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<SrmPurchaseOrderItem>()
            .eq(SrmPurchaseOrderItem::getContractId, contractId)
            .orderByDesc(SrmPurchaseOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));
        Map<String, Object> data = procurementData(Map.of("contractId", contractId), workOrders, purchaseRequests, purchaseOrders, purchaseOrderItems);
        return success("srm_procurement_status", "查询成功", data);
    }

    @Tool("按客户ID、客户编号或客户名称查询相关工单、SRM 采购需求、采购订单和采购明细，返回供应商、合同、工单等展示字段。")
    public String srmGetProcurementByCustomer(String customerIdOrCodeOrName) {
        log.info("【SRM业务工具】srmGetProcurementByCustomer customerIdOrCodeOrName={}", customerIdOrCodeOrName);
        if (StringUtils.isBlank(customerIdOrCodeOrName)) {
            return missing("customer", "请提供客户ID、客户编号或客户名称");
        }
        List<CrmCustomer> customers = findCustomers(customerIdOrCodeOrName.trim());
        if (customers.isEmpty()) {
            return fail("srm_procurement_status", "未找到客户", Map.of("customer", customerIdOrCodeOrName));
        }
        List<Long> customerIds = customers.stream().map(CrmCustomer::getCustomerId).collect(Collectors.toList());
        List<MesWorkOrder> workOrders = workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
            .in(MesWorkOrder::getCustomerId, customerIds)
            .orderByDesc(MesWorkOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<Long> workOrderIds = workOrders.stream().map(MesWorkOrder::getWorkOrderId).collect(Collectors.toList());
        Map<String, Object> data = procurementByWorkOrderIds(workOrderIds, workOrders, Map.of("customer", customerIdOrCodeOrName));
        data.put("customers", customers);
        return success("srm_procurement_status", "查询成功", data);
    }

    @Tool("按供应商ID、供应商编号或供应商名称查询 SRM 采购需求、采购订单和采购明细。")
    public String srmGetProcurementBySupplier(String supplierIdOrCodeOrName) {
        log.info("【SRM业务工具】srmGetProcurementBySupplier supplierIdOrCodeOrName={}", supplierIdOrCodeOrName);
        if (StringUtils.isBlank(supplierIdOrCodeOrName)) {
            return missing("supplier", "请提供供应商ID、供应商编号或供应商名称");
        }
        List<SrmSupplier> suppliers = findSuppliers(supplierIdOrCodeOrName.trim());
        if (suppliers.isEmpty()) {
            return fail("srm_procurement_status", "未找到供应商", Map.of("supplier", supplierIdOrCodeOrName));
        }
        List<Long> supplierIds = suppliers.stream().map(SrmSupplier::getSupplierId).filter(Objects::nonNull).collect(Collectors.toList());
        List<SrmPurchaseRequest> purchaseRequests = purchaseRequestMapper.selectList(new LambdaQueryWrapper<SrmPurchaseRequest>()
            .in(SrmPurchaseRequest::getSupplierId, supplierIds)
            .orderByDesc(SrmPurchaseRequest::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<SrmPurchaseOrder> purchaseOrders = purchaseOrderMapper.selectList(new LambdaQueryWrapper<SrmPurchaseOrder>()
            .in(SrmPurchaseOrder::getSupplierId, supplierIds)
            .orderByDesc(SrmPurchaseOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<Long> workOrderIds = collectWorkOrderIds(purchaseRequests, purchaseOrders, List.of());
        List<MesWorkOrder> workOrders = workOrderIds.isEmpty() ? Collections.emptyList() : workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
            .in(MesWorkOrder::getWorkOrderId, workOrderIds)
            .orderByDesc(MesWorkOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<SrmPurchaseOrderItem> purchaseOrderItems = purchaseOrderItemsByOrders(purchaseOrders);
        Map<String, Object> data = procurementData(Map.of("supplier", supplierIdOrCodeOrName), workOrders, purchaseRequests, purchaseOrders, purchaseOrderItems);
        data.put("suppliers", suppliers);
        return success("srm_procurement_status", "查询成功", data);
    }

    @Tool("按采购需求ID查询 SRM 采购需求、关联采购订单、采购明细和工单线索。")
    public String srmGetPurchaseRequestById(Long purchaseRequestId) {
        log.info("【SRM业务工具】srmGetPurchaseRequestById purchaseRequestId={}", purchaseRequestId);
        if (purchaseRequestId == null) {
            return missing("purchaseRequestId", "请提供采购需求ID");
        }
        SrmPurchaseRequest request = purchaseRequestMapper.selectById(purchaseRequestId);
        if (request == null) {
            return fail("srm_purchase_request", "未找到采购需求", Map.of("purchaseRequestId", purchaseRequestId));
        }
        List<SrmPurchaseOrder> purchaseOrders = purchaseOrderMapper.selectList(new LambdaQueryWrapper<SrmPurchaseOrder>()
            .eq(SrmPurchaseOrder::getPurchaseRequestId, purchaseRequestId)
            .orderByDesc(SrmPurchaseOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<SrmPurchaseOrderItem> purchaseOrderItems = purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<SrmPurchaseOrderItem>()
            .eq(SrmPurchaseOrderItem::getPurchaseRequestId, purchaseRequestId)
            .orderByDesc(SrmPurchaseOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<MesWorkOrder> workOrders = request.getWorkOrderId() == null ? Collections.emptyList() : workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
            .eq(MesWorkOrder::getWorkOrderId, request.getWorkOrderId())
            .last("limit 1"));
        return success("srm_purchase_request", "查询成功", procurementData(Map.of("purchaseRequestId", purchaseRequestId), workOrders, List.of(request), purchaseOrders, purchaseOrderItems));
    }

    @Tool("按采购订单编号查询 SRM 采购订单，并返回关联采购需求、采购明细和工单线索。")
    public String srmGetPurchaseOrderByCode(String purchaseOrderCode) {
        log.info("【SRM业务工具】srmGetPurchaseOrderByCode purchaseOrderCode={}", purchaseOrderCode);
        if (StringUtils.isBlank(purchaseOrderCode)) {
            return missing("purchaseOrderCode", "请提供采购订单编号");
        }
        SrmPurchaseOrder purchaseOrder = purchaseOrderMapper.selectOne(new LambdaQueryWrapper<SrmPurchaseOrder>()
            .eq(SrmPurchaseOrder::getPurchaseOrderCode, purchaseOrderCode.trim())
            .last("limit 1"));
        if (purchaseOrder == null) {
            return fail("srm_purchase_order", "未找到采购订单", Map.of("purchaseOrderCode", purchaseOrderCode));
        }
        List<SrmPurchaseRequest> purchaseRequests = purchaseOrder.getPurchaseRequestId() == null
            ? Collections.emptyList()
            : purchaseRequestMapper.selectList(new LambdaQueryWrapper<SrmPurchaseRequest>()
                .eq(SrmPurchaseRequest::getPurchaseRequestId, purchaseOrder.getPurchaseRequestId())
                .last("limit 1"));
        List<MesWorkOrder> workOrders = purchaseOrder.getWorkOrderId() == null
            ? Collections.emptyList()
            : workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
                .eq(MesWorkOrder::getWorkOrderId, purchaseOrder.getWorkOrderId())
                .last("limit 1"));
        List<SrmPurchaseOrderItem> purchaseOrderItems = purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<SrmPurchaseOrderItem>()
            .eq(SrmPurchaseOrderItem::getPurchaseOrderId, purchaseOrder.getPurchaseOrderId())
            .orderByDesc(SrmPurchaseOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));
        Map<String, Object> data = procurementData(Map.of("purchaseOrderCode", purchaseOrderCode), workOrders, purchaseRequests, List.of(purchaseOrder), purchaseOrderItems);
        return success("srm_purchase_order", "查询成功", data);
    }

    private Map<String, Object> procurementByWorkOrderIds(List<Long> workOrderIds, List<MesWorkOrder> workOrders, Map<String, ?> criterion) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return procurementData(criterion, workOrders, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        List<SrmPurchaseRequest> purchaseRequests = purchaseRequestMapper.selectList(new LambdaQueryWrapper<SrmPurchaseRequest>()
            .in(SrmPurchaseRequest::getWorkOrderId, workOrderIds)
            .orderByDesc(SrmPurchaseRequest::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<SrmPurchaseOrder> purchaseOrders = purchaseOrderMapper.selectList(new LambdaQueryWrapper<SrmPurchaseOrder>()
            .in(SrmPurchaseOrder::getWorkOrderId, workOrderIds)
            .orderByDesc(SrmPurchaseOrder::getCreateTime)
            .last("limit " + MAX_ROWS));
        List<SrmPurchaseOrderItem> purchaseOrderItems = purchaseOrderItemsByOrders(purchaseOrders);
        return procurementData(criterion, workOrders, purchaseRequests, purchaseOrders, purchaseOrderItems);
    }

    private Map<String, Object> procurementData(Map<String, ?> criterion, List<MesWorkOrder> workOrders,
                                                List<SrmPurchaseRequest> purchaseRequests,
                                                List<SrmPurchaseOrder> purchaseOrders,
                                                List<SrmPurchaseOrderItem> purchaseOrderItems) {
        fillDisplayFields(workOrders, purchaseRequests, purchaseOrders, purchaseOrderItems);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("criterion", criterion);
        data.put("summary", Map.of(
            "workOrderCount", workOrders.size(),
            "purchaseRequestCount", purchaseRequests.size(),
            "purchaseOrderCount", purchaseOrders.size(),
            "purchaseOrderItemCount", purchaseOrderItems.size()
        ));
        data.put("workOrders", workOrders);
        data.put("purchaseRequests", purchaseRequests);
        data.put("purchaseOrders", purchaseOrders);
        data.put("purchaseOrderItems", purchaseOrderItems);
        return data;
    }

    private List<SrmPurchaseOrderItem> purchaseOrderItemsByOrders(List<SrmPurchaseOrder> purchaseOrders) {
        List<Long> orderIds = purchaseOrders.stream().map(SrmPurchaseOrder::getPurchaseOrderId).filter(Objects::nonNull).collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<SrmPurchaseOrderItem>()
            .in(SrmPurchaseOrderItem::getPurchaseOrderId, orderIds)
            .orderByDesc(SrmPurchaseOrderItem::getCreateTime)
            .last("limit " + MAX_ROWS));
    }

    private List<Long> collectWorkOrderIds(List<SrmPurchaseRequest> requests, List<SrmPurchaseOrder> orders, List<SrmPurchaseOrderItem> items) {
        List<Long> ids = requests.stream().map(SrmPurchaseRequest::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toList());
        ids.addAll(orders.stream().map(SrmPurchaseOrder::getWorkOrderId).filter(Objects::nonNull).toList());
        ids.addAll(items.stream().map(SrmPurchaseOrderItem::getWorkOrderId).filter(Objects::nonNull).toList());
        return ids.stream().distinct().collect(Collectors.toList());
    }

    private void fillDisplayFields(List<MesWorkOrder> workOrders, List<SrmPurchaseRequest> purchaseRequests,
                                   List<SrmPurchaseOrder> purchaseOrders, List<SrmPurchaseOrderItem> purchaseOrderItems) {
        Set<Long> customerIds = workOrders.stream().map(MesWorkOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = workOrders.stream().map(MesWorkOrder::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        contractIds.addAll(purchaseRequests.stream().map(SrmPurchaseRequest::getContractId).filter(Objects::nonNull).collect(Collectors.toSet()));
        contractIds.addAll(purchaseOrders.stream().map(SrmPurchaseOrder::getContractId).filter(Objects::nonNull).collect(Collectors.toSet()));
        contractIds.addAll(purchaseOrderItems.stream().map(SrmPurchaseOrderItem::getContractId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<Long> supplierIds = purchaseRequests.stream().map(SrmPurchaseRequest::getSupplierId).filter(Objects::nonNull).collect(Collectors.toSet());
        supplierIds.addAll(purchaseOrders.stream().map(SrmPurchaseOrder::getSupplierId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<Long> workOrderIds = workOrders.stream().map(MesWorkOrder::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        workOrderIds.addAll(collectWorkOrderIds(purchaseRequests, purchaseOrders, purchaseOrderItems));
        Set<Long> requestIds = purchaseRequests.stream().map(SrmPurchaseRequest::getPurchaseRequestId).filter(Objects::nonNull).collect(Collectors.toSet());
        requestIds.addAll(purchaseOrders.stream().map(SrmPurchaseOrder::getPurchaseRequestId).filter(Objects::nonNull).collect(Collectors.toSet()));
        requestIds.addAll(purchaseOrderItems.stream().map(SrmPurchaseOrderItem::getPurchaseRequestId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<Long> orderIds = purchaseOrders.stream().map(SrmPurchaseOrder::getPurchaseOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        orderIds.addAll(purchaseOrderItems.stream().map(SrmPurchaseOrderItem::getPurchaseOrderId).filter(Objects::nonNull).collect(Collectors.toSet()));

        Map<Long, CrmCustomer> customers = customerIds.isEmpty() ? Map.of() : customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
            .in(CrmCustomer::getCustomerId, customerIds)).stream().collect(Collectors.toMap(CrmCustomer::getCustomerId, Function.identity(), (a, b) -> a));
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(new LambdaQueryWrapper<CrmContract>()
            .in(CrmContract::getContractId, contractIds)).stream().collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        Map<Long, SrmSupplier> suppliers = supplierIds.isEmpty() ? Map.of() : supplierMapper.selectList(new LambdaQueryWrapper<SrmSupplier>()
            .in(SrmSupplier::getSupplierId, supplierIds)).stream().collect(Collectors.toMap(SrmSupplier::getSupplierId, Function.identity(), (a, b) -> a));
        Map<Long, MesWorkOrder> workOrderMap = workOrderIds.isEmpty() ? Map.of() : workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>()
            .in(MesWorkOrder::getWorkOrderId, workOrderIds)).stream().collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, SrmPurchaseRequest> requestMap = requestIds.isEmpty() ? Map.of() : purchaseRequestMapper.selectList(new LambdaQueryWrapper<SrmPurchaseRequest>()
            .in(SrmPurchaseRequest::getPurchaseRequestId, requestIds)).stream().collect(Collectors.toMap(SrmPurchaseRequest::getPurchaseRequestId, Function.identity(), (a, b) -> a));
        Map<Long, SrmPurchaseOrder> orderMap = orderIds.isEmpty() ? Map.of() : purchaseOrderMapper.selectList(new LambdaQueryWrapper<SrmPurchaseOrder>()
            .in(SrmPurchaseOrder::getPurchaseOrderId, orderIds)).stream().collect(Collectors.toMap(SrmPurchaseOrder::getPurchaseOrderId, Function.identity(), (a, b) -> a));

        workOrders.forEach(workOrder -> fillWorkOrder(workOrder, customers, contracts));
        purchaseRequests.forEach(request -> fillPurchaseRequest(request, workOrderMap, contracts, suppliers));
        purchaseOrders.forEach(order -> fillPurchaseOrder(order, requestMap, suppliers, workOrderMap, contracts));
        purchaseOrderItems.forEach(item -> fillPurchaseOrderItem(item, orderMap, requestMap, workOrderMap, contracts));
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

    private void fillPurchaseRequest(SrmPurchaseRequest request, Map<Long, MesWorkOrder> workOrders,
                                     Map<Long, CrmContract> contracts, Map<Long, SrmSupplier> suppliers) {
        MesWorkOrder workOrder = workOrders.get(request.getWorkOrderId());
        if (workOrder != null) {
            request.setWorkOrderCode(workOrder.getWorkOrderCode());
            request.setProjectName(workOrder.getProjectName());
        }
        CrmContract contract = contracts.get(request.getContractId());
        if (contract != null) {
            request.setContractName(contract.getName());
        }
        SrmSupplier supplier = suppliers.get(request.getSupplierId());
        if (supplier != null) {
            request.setSupplierCode(supplier.getSupplierCode());
            request.setSupplierName(supplier.getSupplierName());
        }
        request.setEngineeringMaterialName(purchaseRequestName(request));
    }

    private void fillPurchaseOrder(SrmPurchaseOrder order, Map<Long, SrmPurchaseRequest> requests,
                                   Map<Long, SrmSupplier> suppliers, Map<Long, MesWorkOrder> workOrders,
                                   Map<Long, CrmContract> contracts) {
        order.setPurchaseRequestName(purchaseRequestName(requests.get(order.getPurchaseRequestId())));
        SrmSupplier supplier = suppliers.get(order.getSupplierId());
        if (supplier != null) {
            order.setSupplierCode(supplier.getSupplierCode());
            order.setSupplierName(supplier.getSupplierName());
        }
        MesWorkOrder workOrder = workOrders.get(order.getWorkOrderId());
        if (workOrder != null) {
            order.setWorkOrderCode(workOrder.getWorkOrderCode());
            order.setProjectName(workOrder.getProjectName());
        }
        CrmContract contract = contracts.get(order.getContractId());
        if (contract != null) {
            order.setContractName(contract.getName());
        }
    }

    private void fillPurchaseOrderItem(SrmPurchaseOrderItem item, Map<Long, SrmPurchaseOrder> orders,
                                       Map<Long, SrmPurchaseRequest> requests, Map<Long, MesWorkOrder> workOrders,
                                       Map<Long, CrmContract> contracts) {
        SrmPurchaseOrder order = orders.get(item.getPurchaseOrderId());
        if (order != null) {
            item.setPurchaseOrderCode(order.getPurchaseOrderCode());
        }
        item.setPurchaseRequestName(purchaseRequestName(requests.get(item.getPurchaseRequestId())));
        MesWorkOrder workOrder = workOrders.get(item.getWorkOrderId());
        if (workOrder != null) {
            item.setWorkOrderCode(workOrder.getWorkOrderCode());
            item.setProjectName(workOrder.getProjectName());
        }
        CrmContract contract = contracts.get(item.getContractId());
        if (contract != null) {
            item.setContractName(contract.getName());
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

    private List<SrmSupplier> findSuppliers(String keyword) {
        Long supplierId = tryParseLong(keyword);
        LambdaQueryWrapper<SrmSupplier> wrapper = new LambdaQueryWrapper<SrmSupplier>()
            .and(w -> {
                if (supplierId != null) {
                    w.eq(SrmSupplier::getSupplierId, supplierId).or();
                }
                w.eq(SrmSupplier::getSupplierCode, keyword)
                    .or()
                    .like(SrmSupplier::getSupplierName, keyword)
                    .or()
                    .like(SrmSupplier::getShortName, keyword);
            })
            .orderByDesc(SrmSupplier::getCreateTime)
            .last("limit " + MAX_ROWS);
        return supplierMapper.selectList(wrapper);
    }

    private String purchaseRequestName(SrmPurchaseRequest request) {
        if (request == null) {
            return null;
        }
        String text = Stream.of(request.getPartCode(), request.getPartName(), request.getWorkOrderCode())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" / "));
        return StringUtils.isNotBlank(text) ? text : "采购需求 #" + request.getPurchaseRequestId();
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
