package org.ruoyi.system.service.wms;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.engineering.EngineeringMaterial;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.wms.WmsIssueOrder;
import org.ruoyi.system.domain.wms.WmsIssueOrderItem;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.engineering.EngineeringMaterialMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.wms.WmsIssueOrderMapper;
import org.ruoyi.system.mapper.wms.WmsIssueOrderItemMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WmsIssueOrderItemService extends XtpCrudService<WmsIssueOrderItem> {

    private final WmsIssueOrderMapper issueOrderMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final CrmContractMapper contractMapper;
    private final EngineeringMaterialMapper engineeringMaterialMapper;

    public WmsIssueOrderItemService(WmsIssueOrderItemMapper mapper, WmsIssueOrderMapper issueOrderMapper,
                                    MesWorkOrderMapper workOrderMapper, CrmContractMapper contractMapper,
                                    EngineeringMaterialMapper engineeringMaterialMapper) {
        super(mapper);
        this.issueOrderMapper = issueOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.contractMapper = contractMapper;
        this.engineeringMaterialMapper = engineeringMaterialMapper;
    }

    @Override
    protected Wrapper<WmsIssueOrderItem> buildQueryWrapper(WmsIssueOrderItem query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<WmsIssueOrderItem>()
            .eq(query.getIssueOrderId() != null, WmsIssueOrderItem::getIssueOrderId, query.getIssueOrderId())
            .eq(query.getWorkOrderId() != null, WmsIssueOrderItem::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, WmsIssueOrderItem::getContractId, query.getContractId())
            .eq(query.getEngineeringMaterialId() != null, WmsIssueOrderItem::getEngineeringMaterialId, query.getEngineeringMaterialId())
            .eq(query.getPartId() != null, WmsIssueOrderItem::getPartId, query.getPartId())
            .like(StringUtils.isNotBlank(query.getPartCode()), WmsIssueOrderItem::getPartCode, query.getPartCode())
            .like(StringUtils.isNotBlank(query.getPartName()), WmsIssueOrderItem::getPartName, query.getPartName())
            .eq(StringUtils.isNotBlank(query.getSpecification()), WmsIssueOrderItem::getSpecification, query.getSpecification())
            .eq(StringUtils.isNotBlank(query.getUnit()), WmsIssueOrderItem::getUnit, query.getUnit())
            .eq(StringUtils.isNotBlank(query.getStatus()), WmsIssueOrderItem::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), WmsIssueOrderItem::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, WmsIssueOrderItem::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(WmsIssueOrderItem::getCreateTime);
    }

    @Override
    protected void fillDisplayFields(List<WmsIssueOrderItem> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> issueOrderIds = records.stream().map(WmsIssueOrderItem::getIssueOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> workOrderIds = records.stream().map(WmsIssueOrderItem::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = records.stream().map(WmsIssueOrderItem::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> engineeringMaterialIds = records.stream().map(WmsIssueOrderItem::getEngineeringMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WmsIssueOrder> issueOrders = issueOrderIds.isEmpty() ? Map.of() : issueOrderMapper.selectList(
            new LambdaQueryWrapper<WmsIssueOrder>().in(WmsIssueOrder::getIssueOrderId, issueOrderIds)
        ).stream().collect(Collectors.toMap(WmsIssueOrder::getIssueOrderId, Function.identity(), (a, b) -> a));
        Map<Long, MesWorkOrder> workOrders = workOrderIds.isEmpty() ? Map.of() : workOrderMapper.selectList(
            new LambdaQueryWrapper<MesWorkOrder>().in(MesWorkOrder::getWorkOrderId, workOrderIds)
        ).stream().collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(
            new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, contractIds)
        ).stream().collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        Map<Long, EngineeringMaterial> engineeringMaterials = engineeringMaterialIds.isEmpty() ? Map.of() : engineeringMaterialMapper.selectList(
            new LambdaQueryWrapper<EngineeringMaterial>().in(EngineeringMaterial::getEngineeringMaterialId, engineeringMaterialIds)
        ).stream().collect(Collectors.toMap(EngineeringMaterial::getEngineeringMaterialId, Function.identity(), (a, b) -> a));
        records.forEach(record -> {
            WmsIssueOrder issueOrder = issueOrders.get(record.getIssueOrderId());
            if (issueOrder != null) {
                record.setIssueOrderName("发料单 #" + issueOrder.getIssueOrderId());
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
            record.setEngineeringMaterialName(engineeringMaterialName(engineeringMaterials.get(record.getEngineeringMaterialId())));
        });
    }

    private String engineeringMaterialName(EngineeringMaterial material) {
        if (material == null) {
            return null;
        }
        return StringUtils.join(List.of(Objects.toString(material.getPartCode(), ""), Objects.toString(material.getPartName(), "")), " / ");
    }
}
