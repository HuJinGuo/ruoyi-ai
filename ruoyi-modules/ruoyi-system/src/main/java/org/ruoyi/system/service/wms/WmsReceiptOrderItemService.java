package org.ruoyi.system.service.wms;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseOrderItem;
import org.ruoyi.system.domain.wms.WmsReceiptOrderItem;
import org.ruoyi.system.domain.wms.WmsReceiptOrder;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderItemMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderMapper;
import org.ruoyi.system.mapper.wms.WmsReceiptOrderItemMapper;
import org.ruoyi.system.mapper.wms.WmsReceiptOrderMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WmsReceiptOrderItemService extends XtpCrudService<WmsReceiptOrderItem> {

    private final WmsReceiptOrderMapper receiptOrderMapper;
    private final SrmPurchaseOrderMapper purchaseOrderMapper;
    private final SrmPurchaseOrderItemMapper purchaseOrderItemMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final CrmContractMapper contractMapper;

    public WmsReceiptOrderItemService(WmsReceiptOrderItemMapper mapper, WmsReceiptOrderMapper receiptOrderMapper,
                                      SrmPurchaseOrderMapper purchaseOrderMapper,
                                      SrmPurchaseOrderItemMapper purchaseOrderItemMapper,
                                      MesWorkOrderMapper workOrderMapper, CrmContractMapper contractMapper) {
        super(mapper);
        this.receiptOrderMapper = receiptOrderMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.workOrderMapper = workOrderMapper;
        this.contractMapper = contractMapper;
    }

    @Override
    protected Wrapper<WmsReceiptOrderItem> buildQueryWrapper(WmsReceiptOrderItem query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<WmsReceiptOrderItem>()
            .eq(query.getReceiptOrderId() != null, WmsReceiptOrderItem::getReceiptOrderId, query.getReceiptOrderId())
            .eq(query.getPurchaseOrderId() != null, WmsReceiptOrderItem::getPurchaseOrderId, query.getPurchaseOrderId())
            .eq(query.getPurchaseOrderItemId() != null, WmsReceiptOrderItem::getPurchaseOrderItemId, query.getPurchaseOrderItemId())
            .eq(query.getWorkOrderId() != null, WmsReceiptOrderItem::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, WmsReceiptOrderItem::getContractId, query.getContractId())
            .eq(query.getPartId() != null, WmsReceiptOrderItem::getPartId, query.getPartId())
            .like(StringUtils.isNotBlank(query.getPartCode()), WmsReceiptOrderItem::getPartCode, query.getPartCode())
            .like(StringUtils.isNotBlank(query.getPartName()), WmsReceiptOrderItem::getPartName, query.getPartName())
            .eq(StringUtils.isNotBlank(query.getSpecification()), WmsReceiptOrderItem::getSpecification, query.getSpecification())
            .eq(StringUtils.isNotBlank(query.getUnit()), WmsReceiptOrderItem::getUnit, query.getUnit())
            .eq(StringUtils.isNotBlank(query.getStatus()), WmsReceiptOrderItem::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), WmsReceiptOrderItem::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, WmsReceiptOrderItem::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(WmsReceiptOrderItem::getCreateTime);
    }

    @Override
    protected void fillDisplayFields(List<WmsReceiptOrderItem> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> receiptOrderIds = records.stream().map(WmsReceiptOrderItem::getReceiptOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> purchaseOrderIds = records.stream().map(WmsReceiptOrderItem::getPurchaseOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> purchaseOrderItemIds = records.stream().map(WmsReceiptOrderItem::getPurchaseOrderItemId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> workOrderIds = records.stream().map(WmsReceiptOrderItem::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = records.stream().map(WmsReceiptOrderItem::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WmsReceiptOrder> receiptOrders = receiptOrderIds.isEmpty() ? Map.of() : receiptOrderMapper.selectList(
            new LambdaQueryWrapper<WmsReceiptOrder>().in(WmsReceiptOrder::getReceiptOrderId, receiptOrderIds)
        ).stream().collect(Collectors.toMap(WmsReceiptOrder::getReceiptOrderId, Function.identity(), (a, b) -> a));
        Map<Long, SrmPurchaseOrder> purchaseOrders = purchaseOrderIds.isEmpty() ? Map.of() : purchaseOrderMapper.selectList(
            new LambdaQueryWrapper<SrmPurchaseOrder>().in(SrmPurchaseOrder::getPurchaseOrderId, purchaseOrderIds)
        ).stream().collect(Collectors.toMap(SrmPurchaseOrder::getPurchaseOrderId, Function.identity(), (a, b) -> a));
        Map<Long, SrmPurchaseOrderItem> purchaseOrderItems = purchaseOrderItemIds.isEmpty() ? Map.of() : purchaseOrderItemMapper.selectList(
            new LambdaQueryWrapper<SrmPurchaseOrderItem>().in(SrmPurchaseOrderItem::getPurchaseOrderItemId, purchaseOrderItemIds)
        ).stream().collect(Collectors.toMap(SrmPurchaseOrderItem::getPurchaseOrderItemId, Function.identity(), (a, b) -> a));
        Map<Long, MesWorkOrder> workOrders = workOrderIds.isEmpty() ? Map.of() : workOrderMapper.selectList(
            new LambdaQueryWrapper<MesWorkOrder>().in(MesWorkOrder::getWorkOrderId, workOrderIds)
        ).stream().collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(
            new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, contractIds)
        ).stream().collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        records.forEach(record -> {
            WmsReceiptOrder receiptOrder = receiptOrders.get(record.getReceiptOrderId());
            if (receiptOrder != null) {
                record.setReceiptOrderName("收料单 #" + receiptOrder.getReceiptOrderId());
            }
            SrmPurchaseOrder purchaseOrder = purchaseOrders.get(record.getPurchaseOrderId());
            if (purchaseOrder != null) {
                record.setPurchaseOrderCode(purchaseOrder.getPurchaseOrderCode());
            }
            record.setPurchaseOrderItemName(purchaseOrderItemName(purchaseOrderItems.get(record.getPurchaseOrderItemId())));
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

    private String purchaseOrderItemName(SrmPurchaseOrderItem item) {
        if (item == null) {
            return null;
        }
        return StringUtils.join(List.of("采购明细 #" + item.getPurchaseOrderItemId(), Objects.toString(item.getPartCode(), ""), Objects.toString(item.getPartName(), "")), " / ");
    }
}
