package org.ruoyi.system.service.wms;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseOrder;
import org.ruoyi.system.domain.srm.SrmSupplier;
import org.ruoyi.system.domain.wms.WmsReceiptOrder;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderMapper;
import org.ruoyi.system.mapper.srm.SrmSupplierMapper;
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
public class WmsReceiptOrderService extends XtpCrudService<WmsReceiptOrder> {

    private final SrmPurchaseOrderMapper purchaseOrderMapper;
    private final SrmSupplierMapper supplierMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final CrmContractMapper contractMapper;
    private final SysUserMapper userMapper;

    public WmsReceiptOrderService(WmsReceiptOrderMapper mapper, SrmPurchaseOrderMapper purchaseOrderMapper,
                                  SrmSupplierMapper supplierMapper, MesWorkOrderMapper workOrderMapper,
                                  CrmContractMapper contractMapper, SysUserMapper userMapper) {
        super(mapper);
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.supplierMapper = supplierMapper;
        this.workOrderMapper = workOrderMapper;
        this.contractMapper = contractMapper;
        this.userMapper = userMapper;
    }

    @Override
    protected Wrapper<WmsReceiptOrder> buildQueryWrapper(WmsReceiptOrder query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<WmsReceiptOrder>()
            .eq(query.getPurchaseOrderId() != null, WmsReceiptOrder::getPurchaseOrderId, query.getPurchaseOrderId())
            .eq(query.getSupplierId() != null, WmsReceiptOrder::getSupplierId, query.getSupplierId())
            .eq(query.getWorkOrderId() != null, WmsReceiptOrder::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, WmsReceiptOrder::getContractId, query.getContractId())
            .eq(StringUtils.isNotBlank(query.getReceiptStatus()), WmsReceiptOrder::getReceiptStatus, query.getReceiptStatus())
            .eq(query.getWarehouseUserId() != null, WmsReceiptOrder::getWarehouseUserId, query.getWarehouseUserId())
            .eq(StringUtils.isNotBlank(query.getRemark()), WmsReceiptOrder::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, WmsReceiptOrder::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(WmsReceiptOrder::getCreateTime);
    }

    @Override
    protected void fillDisplayFields(List<WmsReceiptOrder> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> purchaseOrderIds = records.stream().map(WmsReceiptOrder::getPurchaseOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> supplierIds = records.stream().map(WmsReceiptOrder::getSupplierId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> workOrderIds = records.stream().map(WmsReceiptOrder::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = records.stream().map(WmsReceiptOrder::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = records.stream().map(WmsReceiptOrder::getWarehouseUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SrmPurchaseOrder> purchaseOrders = purchaseOrderIds.isEmpty() ? Map.of() : purchaseOrderMapper.selectList(
            new LambdaQueryWrapper<SrmPurchaseOrder>().in(SrmPurchaseOrder::getPurchaseOrderId, purchaseOrderIds)
        ).stream().collect(Collectors.toMap(SrmPurchaseOrder::getPurchaseOrderId, Function.identity(), (a, b) -> a));
        Map<Long, SrmSupplier> suppliers = supplierIds.isEmpty() ? Map.of() : supplierMapper.selectList(
            new LambdaQueryWrapper<SrmSupplier>().in(SrmSupplier::getSupplierId, supplierIds)
        ).stream().collect(Collectors.toMap(SrmSupplier::getSupplierId, Function.identity(), (a, b) -> a));
        Map<Long, MesWorkOrder> workOrders = workOrderIds.isEmpty() ? Map.of() : workOrderMapper.selectList(
            new LambdaQueryWrapper<MesWorkOrder>().in(MesWorkOrder::getWorkOrderId, workOrderIds)
        ).stream().collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(
            new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, contractIds)
        ).stream().collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of() : userMapper.selectList(
            new LambdaQueryWrapper<SysUser>().in(SysUser::getUserId, userIds)
        ).stream().collect(Collectors.toMap(SysUser::getUserId, Function.identity(), (a, b) -> a));
        records.forEach(record -> {
            SrmPurchaseOrder purchaseOrder = purchaseOrders.get(record.getPurchaseOrderId());
            if (purchaseOrder != null) {
                record.setPurchaseOrderCode(purchaseOrder.getPurchaseOrderCode());
            }
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
            record.setWarehouseUserName(userName(users.get(record.getWarehouseUserId())));
        });
    }

    private String userName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.isNotBlank(user.getNickName()) ? user.getNickName() : user.getUserName();
    }
}
