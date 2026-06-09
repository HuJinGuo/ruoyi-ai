package org.ruoyi.system.service.srm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.engineering.EngineeringMaterial;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.srm.SrmPurchaseRequest;
import org.ruoyi.system.domain.srm.SrmSupplier;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.engineering.EngineeringMaterialMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
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
public class SrmPurchaseRequestService extends XtpCrudService<SrmPurchaseRequest> {

    private final MesWorkOrderMapper workOrderMapper;
    private final CrmContractMapper contractMapper;
    private final EngineeringMaterialMapper engineeringMaterialMapper;
    private final SrmSupplierMapper supplierMapper;

    public SrmPurchaseRequestService(SrmPurchaseRequestMapper mapper, MesWorkOrderMapper workOrderMapper,
                                     CrmContractMapper contractMapper, EngineeringMaterialMapper engineeringMaterialMapper,
                                     SrmSupplierMapper supplierMapper) {
        super(mapper);
        this.workOrderMapper = workOrderMapper;
        this.contractMapper = contractMapper;
        this.engineeringMaterialMapper = engineeringMaterialMapper;
        this.supplierMapper = supplierMapper;
    }

    @Override
    protected Wrapper<SrmPurchaseRequest> buildQueryWrapper(SrmPurchaseRequest query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<SrmPurchaseRequest>()
            .eq(query.getWorkOrderId() != null, SrmPurchaseRequest::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, SrmPurchaseRequest::getContractId, query.getContractId())
            .eq(query.getEngineeringMaterialId() != null, SrmPurchaseRequest::getEngineeringMaterialId, query.getEngineeringMaterialId())
            .eq(query.getSupplierId() != null, SrmPurchaseRequest::getSupplierId, query.getSupplierId())
            .eq(query.getPartId() != null, SrmPurchaseRequest::getPartId, query.getPartId())
            .like(StringUtils.isNotBlank(query.getPartCode()), SrmPurchaseRequest::getPartCode, query.getPartCode())
            .like(StringUtils.isNotBlank(query.getPartName()), SrmPurchaseRequest::getPartName, query.getPartName())
            .eq(StringUtils.isNotBlank(query.getSpecification()), SrmPurchaseRequest::getSpecification, query.getSpecification())
            .eq(StringUtils.isNotBlank(query.getUnit()), SrmPurchaseRequest::getUnit, query.getUnit())
            .eq(StringUtils.isNotBlank(query.getStatus()), SrmPurchaseRequest::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), SrmPurchaseRequest::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, SrmPurchaseRequest::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(SrmPurchaseRequest::getCreateTime);
    }

    @Override
    protected void fillDisplayFields(List<SrmPurchaseRequest> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> workOrderIds = records.stream().map(SrmPurchaseRequest::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = records.stream().map(SrmPurchaseRequest::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> engineeringMaterialIds = records.stream().map(SrmPurchaseRequest::getEngineeringMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> supplierIds = records.stream().map(SrmPurchaseRequest::getSupplierId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MesWorkOrder> workOrders = workOrderIds.isEmpty() ? Map.of() : workOrderMapper.selectList(
            new LambdaQueryWrapper<MesWorkOrder>().in(MesWorkOrder::getWorkOrderId, workOrderIds)
        ).stream().collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(
            new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, contractIds)
        ).stream().collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        Map<Long, EngineeringMaterial> engineeringMaterials = engineeringMaterialIds.isEmpty() ? Map.of() : engineeringMaterialMapper.selectList(
            new LambdaQueryWrapper<EngineeringMaterial>().in(EngineeringMaterial::getEngineeringMaterialId, engineeringMaterialIds)
        ).stream().collect(Collectors.toMap(EngineeringMaterial::getEngineeringMaterialId, Function.identity(), (a, b) -> a));
        Map<Long, SrmSupplier> suppliers = supplierIds.isEmpty() ? Map.of() : supplierMapper.selectList(
            new LambdaQueryWrapper<SrmSupplier>().in(SrmSupplier::getSupplierId, supplierIds)
        ).stream().collect(Collectors.toMap(SrmSupplier::getSupplierId, Function.identity(), (a, b) -> a));
        records.forEach(record -> {
            MesWorkOrder workOrder = workOrders.get(record.getWorkOrderId());
            if (workOrder != null) {
                record.setWorkOrderCode(workOrder.getWorkOrderCode());
                record.setProjectName(workOrder.getProjectName());
            }
            CrmContract contract = contracts.get(record.getContractId());
            if (contract != null) {
                record.setContractName(contract.getName());
            }
            record.setEngineeringMaterialName(engineeringMaterialName(engineeringMaterials.get(record.getEngineeringMaterialId())));
            SrmSupplier supplier = suppliers.get(record.getSupplierId());
            if (supplier != null) {
                record.setSupplierCode(supplier.getSupplierCode());
                record.setSupplierName(supplier.getSupplierName());
            }
        });
    }

    private String engineeringMaterialName(EngineeringMaterial material) {
        if (material == null) {
            return null;
        }
        return StringUtils.join(List.of(Objects.toString(material.getPartCode(), ""), Objects.toString(material.getPartName(), "")), " / ");
    }
}
