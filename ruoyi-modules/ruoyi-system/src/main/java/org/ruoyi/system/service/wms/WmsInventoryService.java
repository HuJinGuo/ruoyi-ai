package org.ruoyi.system.service.wms;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.wms.WmsInventory;
import org.ruoyi.system.mapper.wms.WmsInventoryMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WmsInventoryService extends XtpCrudService<WmsInventory> {

    public WmsInventoryService(WmsInventoryMapper mapper) {
        super(mapper);
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
}
