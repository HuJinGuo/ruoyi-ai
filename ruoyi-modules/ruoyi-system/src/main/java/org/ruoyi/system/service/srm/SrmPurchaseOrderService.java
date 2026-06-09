package org.ruoyi.system.service.srm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseRequest;
import org.ruoyi.system.domain.srm.SrmSupplier;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseRequestMapper;
import org.ruoyi.system.mapper.srm.SrmSupplierMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SrmPurchaseOrderService extends XtpCrudService<SrmPurchaseOrder> {

    private final SrmPurchaseRequestMapper purchaseRequestMapper;
    private final SrmSupplierMapper supplierMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final CrmContractMapper contractMapper;

    public SrmPurchaseOrderService(SrmPurchaseOrderMapper mapper, SrmPurchaseRequestMapper purchaseRequestMapper,
                                   SrmSupplierMapper supplierMapper, MesWorkOrderMapper workOrderMapper,
                                   CrmContractMapper contractMapper) {
        super(mapper);
        this.purchaseRequestMapper = purchaseRequestMapper;
        this.supplierMapper = supplierMapper;
        this.workOrderMapper = workOrderMapper;
        this.contractMapper = contractMapper;
    }

    @Override
    protected Wrapper<SrmPurchaseOrder> buildQueryWrapper(SrmPurchaseOrder query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<SrmPurchaseOrder>()
            .like(StringUtils.isNotBlank(query.getPurchaseOrderCode()), SrmPurchaseOrder::getPurchaseOrderCode, query.getPurchaseOrderCode())
            .eq(query.getPurchaseRequestId() != null, SrmPurchaseOrder::getPurchaseRequestId, query.getPurchaseRequestId())
            .eq(query.getSupplierId() != null, SrmPurchaseOrder::getSupplierId, query.getSupplierId())
            .eq(query.getWorkOrderId() != null, SrmPurchaseOrder::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, SrmPurchaseOrder::getContractId, query.getContractId())
            .eq(StringUtils.isNotBlank(query.getStatus()), SrmPurchaseOrder::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), SrmPurchaseOrder::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, SrmPurchaseOrder::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(SrmPurchaseOrder::getCreateTime);
    }

    @Override
    protected void fillDisplayFields(List<SrmPurchaseOrder> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> purchaseRequestIds = records.stream().map(SrmPurchaseOrder::getPurchaseRequestId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> supplierIds = records.stream().map(SrmPurchaseOrder::getSupplierId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> workOrderIds = records.stream().map(SrmPurchaseOrder::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = records.stream().map(SrmPurchaseOrder::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SrmPurchaseRequest> purchaseRequests = purchaseRequestIds.isEmpty() ? Map.of() : purchaseRequestMapper.selectList(
            new LambdaQueryWrapper<SrmPurchaseRequest>().in(SrmPurchaseRequest::getPurchaseRequestId, purchaseRequestIds)
        ).stream().collect(Collectors.toMap(SrmPurchaseRequest::getPurchaseRequestId, Function.identity(), (a, b) -> a));
        Map<Long, SrmSupplier> suppliers = supplierIds.isEmpty() ? Map.of() : supplierMapper.selectList(
            new LambdaQueryWrapper<SrmSupplier>().in(SrmSupplier::getSupplierId, supplierIds)
        ).stream().collect(Collectors.toMap(SrmSupplier::getSupplierId, Function.identity(), (a, b) -> a));
        Map<Long, MesWorkOrder> workOrders = workOrderIds.isEmpty() ? Map.of() : workOrderMapper.selectList(
            new LambdaQueryWrapper<MesWorkOrder>().in(MesWorkOrder::getWorkOrderId, workOrderIds)
        ).stream().collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(
            new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, contractIds)
        ).stream().collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        records.forEach(record -> {
            record.setPurchaseRequestName(purchaseRequestName(purchaseRequests.get(record.getPurchaseRequestId())));
            SrmSupplier supplier = suppliers.get(record.getSupplierId());
            if (supplier != null) {
                record.setSupplierCode(supplier.getSupplierCode());
                record.setSupplierName(supplier.getSupplierName());
            }
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
