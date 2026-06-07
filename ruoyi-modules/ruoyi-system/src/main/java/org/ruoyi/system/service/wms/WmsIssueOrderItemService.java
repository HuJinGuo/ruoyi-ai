package org.ruoyi.system.service.wms;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.wms.WmsIssueOrderItem;
import org.ruoyi.system.mapper.wms.WmsIssueOrderItemMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WmsIssueOrderItemService extends XtpCrudService<WmsIssueOrderItem> {

    public WmsIssueOrderItemService(WmsIssueOrderItemMapper mapper) {
        super(mapper);
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
}
