package org.ruoyi.agent.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.engineering.EngineeringMaterial;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.engineering.EngineeringMaterialMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
public class EngineeringBusinessTool {

    private final MesWorkOrderMapper workOrderMapper;
    private final EngineeringMaterialMapper engineeringMaterialMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmContractMapper contractMapper;

    @Tool("按工单编号查询工程物料清算和缺料情况。")
    public String engineeringGetMaterialsByWorkOrderCode(String workOrderCode) {
        log.info("【工程业务工具】engineeringGetMaterialsByWorkOrderCode workOrderCode={}", workOrderCode);
        return engineeringQueryMaterials(workOrderCode, null, null, null, null);
    }

    @Tool("按工单编号、工单ID、合同ID、客户ID、客户编号或客户名称查询工程物料清单和缺料。没有 workOrderCode 时也应使用本工具。")
    public String engineeringQueryMaterials(String workOrderCode, String workOrderId, String contractId,
                                            String customerId, String customerCodeOrName) {
        log.info("【工程业务工具】engineeringQueryMaterials workOrderCode={}, workOrderId={}, contractId={}, customerId={}, customer={}",
            workOrderCode, workOrderId, contractId, customerId, customerCodeOrName);
        WorkOrderSearchResult searchResult = findWorkOrders(workOrderCode, workOrderId, contractId, customerId, customerCodeOrName);
        if (!searchResult.success()) {
            return toJson(searchResult.payload());
        }

        List<MesWorkOrder> workOrders = searchResult.workOrders();
        fillWorkOrderDisplayFields(workOrders);
        List<EngineeringMaterial> materials = findMaterials(workOrders, parseLong(contractId));
        fillMaterialDisplayFields(materials, workOrders);
        List<EngineeringMaterial> shortages = materials.stream()
            .filter(material -> material.getShortageQty() != null && material.getShortageQty().compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toList());

        if (workOrders.isEmpty() && materials.isEmpty()) {
            return toJson(result(false, "engineering_materials", "未找到符合条件的工单或工程物料",
                Map.of("query", searchResult.query())));
        }

        Map<Long, List<EngineeringMaterial>> materialsByWorkOrder = materials.stream()
            .filter(material -> material.getWorkOrderId() != null)
            .collect(Collectors.groupingBy(EngineeringMaterial::getWorkOrderId));
        List<Map<String, Object>> records = workOrders.stream()
            .map(workOrder -> {
                List<EngineeringMaterial> workOrderMaterials = materialsByWorkOrder.getOrDefault(workOrder.getWorkOrderId(), List.of());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("workOrder", workOrder);
                item.put("materials", workOrderMaterials);
                item.put("shortages", workOrderMaterials.stream()
                    .filter(material -> material.getShortageQty() != null && material.getShortageQty().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList()));
                return item;
            })
            .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("query", searchResult.query());
        data.put("workOrderCount", workOrders.size());
        data.put("materialCount", materials.size());
        data.put("shortageCount", shortages.size());
        data.put("workOrders", records);
        data.put("materials", materials);
        data.put("shortages", shortages);
        return toJson(result(true, "engineering_materials", "查询成功", data));
    }

    private WorkOrderSearchResult findWorkOrders(String workOrderCode, String workOrderId, String contractId,
                                                 String customerId, String customerCodeOrName) {
        Map<String, Object> query = new LinkedHashMap<>();
        putIfNotBlank(query, "workOrderCode", workOrderCode);
        putIfNotBlank(query, "workOrderId", workOrderId);
        putIfNotBlank(query, "contractId", contractId);
        putIfNotBlank(query, "customerId", customerId);
        putIfNotBlank(query, "customerCodeOrName", customerCodeOrName);

        Long parsedWorkOrderId = parseLong(workOrderId);
        Long parsedContractId = parseLong(contractId);
        Long parsedCustomerId = parseLong(customerId);

        if (StringUtils.isNotBlank(workOrderId) && parsedWorkOrderId == null) {
            return WorkOrderSearchResult.failure(missing("workOrderId", "工单ID必须是数字", query));
        }
        if (StringUtils.isNotBlank(contractId) && parsedContractId == null) {
            return WorkOrderSearchResult.failure(missing("contractId", "合同ID必须是数字", query));
        }
        if (StringUtils.isNotBlank(customerId) && parsedCustomerId == null) {
            return WorkOrderSearchResult.failure(missing("customerId", "客户ID必须是数字", query));
        }

        Set<Long> customerIds = resolveCustomerIds(parsedCustomerId, customerCodeOrName);
        if (StringUtils.isNotBlank(customerCodeOrName) && customerIds.isEmpty()) {
            return WorkOrderSearchResult.failure(result(false, "engineering_materials", "未找到匹配的客户",
                Map.of("query", query, "missing", "customer", "need", "请提供客户ID、准确客户编号或客户名称")));
        }

        if (query.isEmpty()) {
            return WorkOrderSearchResult.failure(result(false, "engineering_materials", "缺少查询条件",
                Map.of("missing", "query", "need", "请提供工单编号、工单ID、合同ID、客户ID、客户编号或客户名称")));
        }

        LambdaQueryWrapper<MesWorkOrder> wrapper = new LambdaQueryWrapper<MesWorkOrder>()
            .eq(StringUtils.isNotBlank(workOrderCode), MesWorkOrder::getWorkOrderCode, workOrderCode)
            .eq(parsedWorkOrderId != null, MesWorkOrder::getWorkOrderId, parsedWorkOrderId)
            .eq(parsedContractId != null, MesWorkOrder::getContractId, parsedContractId)
            .eq(parsedCustomerId != null, MesWorkOrder::getCustomerId, parsedCustomerId)
            .in(!customerIds.isEmpty(), MesWorkOrder::getCustomerId, customerIds)
            .orderByDesc(MesWorkOrder::getCreateTime)
            .last("limit 50");
        return WorkOrderSearchResult.success(workOrderMapper.selectList(wrapper), query);
    }

    private List<EngineeringMaterial> findMaterials(List<MesWorkOrder> workOrders, Long contractId) {
        List<Long> workOrderIds = workOrders.stream()
            .map(MesWorkOrder::getWorkOrderId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        LambdaQueryWrapper<EngineeringMaterial> wrapper = new LambdaQueryWrapper<EngineeringMaterial>();
        if (!workOrderIds.isEmpty() && contractId != null) {
            wrapper.and(nested -> nested.in(EngineeringMaterial::getWorkOrderId, workOrderIds)
                .or()
                .eq(EngineeringMaterial::getContractId, contractId));
        } else if (!workOrderIds.isEmpty()) {
            wrapper.in(EngineeringMaterial::getWorkOrderId, workOrderIds);
        } else if (contractId != null) {
            wrapper.eq(EngineeringMaterial::getContractId, contractId);
        } else {
            return List.of();
        }
        return engineeringMaterialMapper.selectList(wrapper.orderByDesc(EngineeringMaterial::getCreateTime));
    }

    private Set<Long> resolveCustomerIds(Long customerId, String customerCodeOrName) {
        if (customerId != null) {
            return Set.of(customerId);
        }
        if (StringUtils.isBlank(customerCodeOrName)) {
            return Set.of();
        }
        String keyword = customerCodeOrName.trim();
        Set<Long> customerIds = customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
                .eq(CrmCustomer::getCode, keyword)
                .or()
                .like(CrmCustomer::getName, keyword)
                .or()
                .like(CrmCustomer::getShortName, keyword)
                .last("limit 20"))
            .stream()
            .map(CrmCustomer::getCustomerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Long keywordId = parseLong(keyword);
        if (keywordId != null) {
            customerIds.add(keywordId);
        }
        return customerIds;
    }

    private void fillWorkOrderDisplayFields(List<MesWorkOrder> workOrders) {
        Set<Long> customerIds = workOrders.stream().map(MesWorkOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = workOrders.stream().map(MesWorkOrder::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, CrmCustomer> customers = customerIds.isEmpty() ? Map.of() : customerMapper.selectList(
                new LambdaQueryWrapper<CrmCustomer>().in(CrmCustomer::getCustomerId, customerIds))
            .stream()
            .collect(Collectors.toMap(CrmCustomer::getCustomerId, Function.identity(), (a, b) -> a));
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(
                new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, contractIds))
            .stream()
            .collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        workOrders.forEach(workOrder -> {
            CrmCustomer customer = customers.get(workOrder.getCustomerId());
            if (customer != null) {
                workOrder.setCustomerCode(customer.getCode());
                workOrder.setCustomerName(customer.getName());
            }
            CrmContract contract = contracts.get(workOrder.getContractId());
            if (contract != null) {
                workOrder.setContractName(contract.getName());
            }
        });
    }

    private void fillMaterialDisplayFields(List<EngineeringMaterial> materials, List<MesWorkOrder> knownWorkOrders) {
        Set<Long> workOrderIds = materials.stream().map(EngineeringMaterial::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = materials.stream().map(EngineeringMaterial::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MesWorkOrder> workOrders = knownWorkOrders.stream()
            .filter(workOrder -> workOrder.getWorkOrderId() != null)
            .collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        if (!workOrderIds.isEmpty() && workOrders.size() < workOrderIds.size()) {
            workOrders.putAll(workOrderMapper.selectList(new LambdaQueryWrapper<MesWorkOrder>().in(MesWorkOrder::getWorkOrderId, workOrderIds))
                .stream()
                .collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a)));
        }
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(
                new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, contractIds))
            .stream()
            .collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        materials.forEach(material -> {
            MesWorkOrder workOrder = workOrders.get(material.getWorkOrderId());
            if (workOrder != null) {
                material.setWorkOrderCode(workOrder.getWorkOrderCode());
                material.setProjectName(workOrder.getProjectName());
            }
            CrmContract contract = contracts.get(material.getContractId());
            if (contract != null) {
                material.setContractName(contract.getName());
            }
        });
    }

    private Map<String, Object> result(boolean success, String type, String message, Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("type", type);
        result.put("message", message);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> missing(String field, String message, Map<String, Object> query) {
        return result(false, "engineering_materials", message, Map.of("query", query, "missing", field, "need", message));
    }

    private void putIfNotBlank(Map<String, Object> map, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            map.put(key, value.trim());
        }
    }

    private Long parseLong(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toJson(Object value) {
        return JsonUtils.toJsonString(value);
    }

    private record WorkOrderSearchResult(boolean success, List<MesWorkOrder> workOrders,
                                         Map<String, Object> query, Map<String, Object> payload) {
        static WorkOrderSearchResult success(List<MesWorkOrder> workOrders, Map<String, Object> query) {
            return new WorkOrderSearchResult(true, workOrders, query, null);
        }

        static WorkOrderSearchResult failure(Map<String, Object> payload) {
            return new WorkOrderSearchResult(false, List.of(), Map.of(), payload);
        }
    }
}
