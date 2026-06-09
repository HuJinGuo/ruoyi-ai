package org.ruoyi.system.service.srm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseOrderItem;
import org.ruoyi.system.domain.srm.SrmPurchaseOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseRequest;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderItemMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseRequestMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SrmPurchaseOrderItemService extends XtpCrudService<SrmPurchaseOrderItem> {

    private final SrmPurchaseOrderMapper purchaseOrderMapper;
    private final SrmPurchaseRequestMapper purchaseRequestMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final CrmContractMapper contractMapper;

    public SrmPurchaseOrderItemService(SrmPurchaseOrderItemMapper mapper, SrmPurchaseOrderMapper purchaseOrderMapper,
                                       SrmPurchaseRequestMapper purchaseRequestMapper, MesWorkOrderMapper workOrderMapper,
                                       CrmContractMapper contractMapper) {
        super(mapper);
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseRequestMapper = purchaseRequestMapper;
        this.workOrderMapper = workOrderMapper;
        this.contractMapper = contractMapper;
    }

    @Override
    protected Wrapper<SrmPurchaseOrderItem> buildQueryWrapper(SrmPurchaseOrderItem query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<SrmPurchaseOrderItem>()
            .eq(query.getPurchaseOrderId() != null, SrmPurchaseOrderItem::getPurchaseOrderId, query.getPurchaseOrderId())
            .eq(query.getPurchaseRequestId() != null, SrmPurchaseOrderItem::getPurchaseRequestId, query.getPurchaseRequestId())
            .eq(query.getWorkOrderId() != null, SrmPurchaseOrderItem::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, SrmPurchaseOrderItem::getContractId, query.getContractId())
            .eq(query.getPartId() != null, SrmPurchaseOrderItem::getPartId, query.getPartId())
            .like(StringUtils.isNotBlank(query.getPartCode()), SrmPurchaseOrderItem::getPartCode, query.getPartCode())
            .like(StringUtils.isNotBlank(query.getPartName()), SrmPurchaseOrderItem::getPartName, query.getPartName())
            .eq(StringUtils.isNotBlank(query.getSpecification()), SrmPurchaseOrderItem::getSpecification, query.getSpecification())
            .eq(StringUtils.isNotBlank(query.getUnit()), SrmPurchaseOrderItem::getUnit, query.getUnit())
            .eq(StringUtils.isNotBlank(query.getStatus()), SrmPurchaseOrderItem::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), SrmPurchaseOrderItem::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, SrmPurchaseOrderItem::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(SrmPurchaseOrderItem::getCreateTime);
    }

    @Override
    protected void fillDisplayFields(List<SrmPurchaseOrderItem> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> purchaseOrderIds = records.stream().map(SrmPurchaseOrderItem::getPurchaseOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> purchaseRequestIds = records.stream().map(SrmPurchaseOrderItem::getPurchaseRequestId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> workOrderIds = records.stream().map(SrmPurchaseOrderItem::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = records.stream().map(SrmPurchaseOrderItem::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SrmPurchaseOrder> purchaseOrders = purchaseOrderIds.isEmpty() ? Map.of() : purchaseOrderMapper.selectList(
            new LambdaQueryWrapper<SrmPurchaseOrder>().in(SrmPurchaseOrder::getPurchaseOrderId, purchaseOrderIds)
        ).stream().collect(Collectors.toMap(SrmPurchaseOrder::getPurchaseOrderId, Function.identity(), (a, b) -> a));
        Map<Long, SrmPurchaseRequest> purchaseRequests = purchaseRequestIds.isEmpty() ? Map.of() : purchaseRequestMapper.selectList(
            new LambdaQueryWrapper<SrmPurchaseRequest>().in(SrmPurchaseRequest::getPurchaseRequestId, purchaseRequestIds)
        ).stream().collect(Collectors.toMap(SrmPurchaseRequest::getPurchaseRequestId, Function.identity(), (a, b) -> a));
        Map<Long, MesWorkOrder> workOrders = workOrderIds.isEmpty() ? Map.of() : workOrderMapper.selectList(
            new LambdaQueryWrapper<MesWorkOrder>().in(MesWorkOrder::getWorkOrderId, workOrderIds)
        ).stream().collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(
            new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, contractIds)
        ).stream().collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        records.forEach(record -> {
            SrmPurchaseOrder purchaseOrder = purchaseOrders.get(record.getPurchaseOrderId());
            if (purchaseOrder != null) {
                record.setPurchaseOrderCode(purchaseOrder.getPurchaseOrderCode());
            }
            record.setPurchaseRequestName(purchaseRequestName(purchaseRequests.get(record.getPurchaseRequestId())));
            MesWorkOrder workOrder = workOrders.get(record.getWorkOrderId());
            if (workOrder != null) {
                record.setWorkOrderCode(workOrder.getWorkOrderCode());
                record.setProjectName(workOrder.getProjectName());
            }
            CrmContract contract = contracts.get(record.getContractId());
            if (contract != null) {
                record.setContractName(contract.getName());
            }
        });
    }

    private String purchaseRequestName(SrmPurchaseRequest request) {
        return request == null ? null : "采购需求 #" + request.getPurchaseRequestId();
    }
}
