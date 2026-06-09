package org.ruoyi.system.service.engineering;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.engineering.EngineeringMaterial;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.engineering.EngineeringMaterialMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EngineeringMaterialService extends XtpCrudService<EngineeringMaterial> {

    private final MesWorkOrderMapper workOrderMapper;
    private final CrmContractMapper contractMapper;

    public EngineeringMaterialService(EngineeringMaterialMapper mapper, MesWorkOrderMapper workOrderMapper,
                                      CrmContractMapper contractMapper) {
        super(mapper);
        this.workOrderMapper = workOrderMapper;
        this.contractMapper = contractMapper;
    }

    @Override
    protected Wrapper<EngineeringMaterial> buildQueryWrapper(EngineeringMaterial query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<EngineeringMaterial>()
            .eq(query.getWorkOrderId() != null, EngineeringMaterial::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, EngineeringMaterial::getContractId, query.getContractId())
            .eq(query.getPartId() != null, EngineeringMaterial::getPartId, query.getPartId())
            .like(StringUtils.isNotBlank(query.getPartCode()), EngineeringMaterial::getPartCode, query.getPartCode())
            .like(StringUtils.isNotBlank(query.getPartName()), EngineeringMaterial::getPartName, query.getPartName())
            .eq(StringUtils.isNotBlank(query.getSpecification()), EngineeringMaterial::getSpecification, query.getSpecification())
            .eq(StringUtils.isNotBlank(query.getUnit()), EngineeringMaterial::getUnit, query.getUnit())
            .eq(StringUtils.isNotBlank(query.getStatus()), EngineeringMaterial::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), EngineeringMaterial::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, EngineeringMaterial::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(EngineeringMaterial::getCreateTime);
    }

    @Override
    protected void fillDisplayFields(List<EngineeringMaterial> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> workOrderIds = records.stream().map(EngineeringMaterial::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = records.stream().map(EngineeringMaterial::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MesWorkOrder> workOrders = workOrderIds.isEmpty() ? Map.of() : workOrderMapper.selectList(
            new LambdaQueryWrapper<MesWorkOrder>().in(MesWorkOrder::getWorkOrderId, workOrderIds)
        ).stream().collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(
            new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, contractIds)
        ).stream().collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
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
        });
    }
}
