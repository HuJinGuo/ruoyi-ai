package org.ruoyi.system.service.material;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.material.MaterialPart;
import org.ruoyi.system.domain.srm.SrmSupplier;
import org.ruoyi.system.mapper.material.MaterialPartMapper;
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
public class MaterialPartService extends XtpCrudService<MaterialPart> {

    private final SrmSupplierMapper supplierMapper;

    public MaterialPartService(MaterialPartMapper mapper, SrmSupplierMapper supplierMapper) {
        super(mapper);
        this.supplierMapper = supplierMapper;
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

    @Override
    protected void fillDisplayFields(List<MaterialPart> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> supplierIds = records.stream()
            .map(MaterialPart::getDefaultSupplierId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, SrmSupplier> suppliers = supplierIds.isEmpty() ? Map.of() : supplierMapper.selectList(
            new LambdaQueryWrapper<SrmSupplier>().in(SrmSupplier::getSupplierId, supplierIds)
        ).stream().collect(Collectors.toMap(SrmSupplier::getSupplierId, Function.identity(), (a, b) -> a));
        records.forEach(record -> {
            SrmSupplier supplier = suppliers.get(record.getDefaultSupplierId());
            if (supplier != null) {
                record.setDefaultSupplierCode(supplier.getSupplierCode());
                record.setDefaultSupplierName(supplier.getSupplierName());
            }
        });
    }
}
