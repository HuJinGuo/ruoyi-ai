package org.ruoyi.system.service.wms;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.material.MaterialPart;
import org.ruoyi.system.domain.wms.WmsInventory;
import org.ruoyi.system.mapper.material.MaterialPartMapper;
import org.ruoyi.system.mapper.wms.WmsInventoryMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WmsInventoryService extends XtpCrudService<WmsInventory> {

    private final MaterialPartMapper materialPartMapper;

    public WmsInventoryService(WmsInventoryMapper mapper, MaterialPartMapper materialPartMapper) {
        super(mapper);
        this.materialPartMapper = materialPartMapper;
    }

    @Override
    protected Wrapper<WmsInventory> buildQueryWrapper(WmsInventory query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<WmsInventory>()
            .eq(query.getPartId() != null, WmsInventory::getPartId, query.getPartId())
            .like(StringUtils.isNotBlank(query.getPartCode()), WmsInventory::getPartCode, query.getPartCode())
            .like(StringUtils.isNotBlank(query.getPartName()), WmsInventory::getPartName, query.getPartName())
            .eq(StringUtils.isNotBlank(query.getSpecification()), WmsInventory::getSpecification, query.getSpecification())
            .eq(StringUtils.isNotBlank(query.getUnit()), WmsInventory::getUnit, query.getUnit())
            .like(StringUtils.isNotBlank(query.getLocationCode()), WmsInventory::getLocationCode, query.getLocationCode())
            .eq(StringUtils.isNotBlank(query.getRemark()), WmsInventory::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, WmsInventory::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(WmsInventory::getCreateTime);
    }

    @Override
    protected void fillDisplayFields(List<WmsInventory> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> partIds = records.stream().map(WmsInventory::getPartId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MaterialPart> parts = partIds.isEmpty() ? Map.of() : materialPartMapper.selectList(
            new LambdaQueryWrapper<MaterialPart>().in(MaterialPart::getPartId, partIds)
        ).stream().collect(Collectors.toMap(MaterialPart::getPartId, Function.identity(), (a, b) -> a));
        records.forEach(record -> {
            MaterialPart part = parts.get(record.getPartId());
            if (part != null) {
                record.setPartCode(part.getPartCode());
                record.setPartName(part.getPartName());
                record.setSpecification(part.getSpecification());
                record.setUnit(part.getUnit());
            }
        });
    }
}
