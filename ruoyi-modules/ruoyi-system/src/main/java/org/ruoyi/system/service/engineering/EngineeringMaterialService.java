package org.ruoyi.system.service.engineering;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.engineering.EngineeringMaterial;
import org.ruoyi.system.mapper.engineering.EngineeringMaterialMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EngineeringMaterialService extends XtpCrudService<EngineeringMaterial> {

    public EngineeringMaterialService(EngineeringMaterialMapper mapper) {
        super(mapper);
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
}
