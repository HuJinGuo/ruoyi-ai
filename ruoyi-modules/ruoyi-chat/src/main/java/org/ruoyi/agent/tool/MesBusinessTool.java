package org.ruoyi.agent.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.mes.MesWorkOrderStage;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderStageMapper;
import org.springframework.stereotype.Component;

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
public class MesBusinessTool {

    private final MesWorkOrderMapper workOrderMapper;
    private final MesWorkOrderStageMapper stageMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmContractMapper contractMapper;

    @Tool("按工单编号查询 MES 工单进度和节点。优先使用 workOrderCode。")
    public String mesGetWorkOrderProgress(String workOrderCode) {
        log.info("【MES业务工具】mesGetWorkOrderProgress workOrderCode={}", workOrderCode);
        return mesQueryWorkOrders(workOrderCode, null, null, null, null);
    }

    @Tool("按工单编号、工单ID、合同ID、客户ID、客户编号或客户名称查询 MES 工单及阶段。没有 workOrderCode 时也应使用本工具。")
    public String mesQueryWorkOrders(String workOrderCode, String workOrderId, String contractId,
                                     String customerId, String customerCodeOrName) {
        log.info("【MES业务工具】mesQueryWorkOrders workOrderCode={}, workOrderId={}, contractId={}, customerId={}, customer={}",
            workOrderCode, workOrderId, contractId, customerId, customerCodeOrName);
        WorkOrderSearchResult searchResult = findWorkOrders(workOrderCode, workOrderId, contractId, customerId, customerCodeOrName);
        if (!searchResult.success()) {
            return toJson(searchResult.payload());
        }

        List<MesWorkOrder> workOrders = searchResult.workOrders();
        if (workOrders.isEmpty()) {
            return toJson(result(false, "mes_work_orders", "未找到符合条件的 MES 工单",
                Map.of("query", searchResult.query())));
        }
        fillWorkOrderDisplayFields(workOrders);

        List<Map<String, Object>> records = workOrders.stream()
            .map(workOrder -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("workOrder", workOrder);
                item.put("stages", findStages(workOrder.getWorkOrderId()));
                return item;
            })
            .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("query", searchResult.query());
        data.put("count", records.size());
        data.put("workOrders", records);
        return toJson(result(true, "mes_work_orders", "查询成功", data));
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
            return WorkOrderSearchResult.failure(result(false, "mes_work_orders", "未找到匹配的客户",
                Map.of("query", query, "missing", "customer", "need", "请提供客户ID、准确客户编号或客户名称")));
        }

        if (query.isEmpty()) {
            return WorkOrderSearchResult.failure(result(false, "mes_work_orders", "缺少查询条件",
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

    private List<MesWorkOrderStage> findStages(Long workOrderId) {
        return stageMapper.selectList(new LambdaQueryWrapper<MesWorkOrderStage>()
            .eq(MesWorkOrderStage::getWorkOrderId, workOrderId)
            .orderByAsc(MesWorkOrderStage::getCreateTime));
    }

    private Set<Long> resolveCustomerIds(Long customerId, String customerCodeOrName) {
        if (customerId != null) {
            return Set.of(customerId);
        }
        if (StringUtils.isBlank(customerCodeOrName)) {
            return Set.of();
        }
        String keyword = customerCodeOrName.trim();
        return customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
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

    private Map<String, Object> result(boolean success, String type, String message, Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("type", type);
        result.put("message", message);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> missing(String field, String message, Map<String, Object> query) {
        return result(false, "mes_work_orders", message, Map.of("query", query, "missing", field, "need", message));
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
