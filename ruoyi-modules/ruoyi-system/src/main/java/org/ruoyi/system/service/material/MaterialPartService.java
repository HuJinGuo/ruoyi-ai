package org.ruoyi.system.service.material;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.material.MaterialPart;
import org.ruoyi.system.mapper.material.MaterialPartMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MaterialPartService extends XtpCrudService<MaterialPart> {

    public MaterialPartService(MaterialPartMapper mapper) {
        super(mapper);
    }

    @Override
    protected Wrapper<MaterialPart> buildQueryWrapper(MaterialPart query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<MaterialPart>()
            .like(StringUtils.isNotBlank(query.getPartCode()), MaterialPart::getPartCode, query.getPartCode())
            .like(StringUtils.isNotBlank(query.getPartName()), MaterialPart::getPartName, query.getPartName())
            .eq(StringUtils.isNotBlank(query.getSpecification()), MaterialPart::getSpecification, query.getSpecification())
            .eq(StringUtils.isNotBlank(query.getUnit()), MaterialPart::getUnit, query.getUnit())
            .eq(StringUtils.isNotBlank(query.getMaterial()), MaterialPart::getMaterial, query.getMaterial())
            .eq(StringUtils.isNotBlank(query.getCategory()), MaterialPart::getCategory, query.getCategory())
            .eq(query.getDefaultSupplierId() != null, MaterialPart::getDefaultSupplierId, query.getDefaultSupplierId())
            .eq(StringUtils.isNotBlank(query.getStatus()), MaterialPart::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), MaterialPart::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, MaterialPart::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(MaterialPart::getCreateTime);
    }
}
