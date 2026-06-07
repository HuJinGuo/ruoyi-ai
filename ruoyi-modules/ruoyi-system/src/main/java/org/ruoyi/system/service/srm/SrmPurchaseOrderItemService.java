package org.ruoyi.system.service.srm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.srm.SrmPurchaseOrderItem;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderItemMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SrmPurchaseOrderItemService extends XtpCrudService<SrmPurchaseOrderItem> {

    public SrmPurchaseOrderItemService(SrmPurchaseOrderItemMapper mapper) {
        super(mapper);
    }

    @Override
    protected Wrapper<SrmPurchaseOrderItem> buildQueryWrapper(SrmPurchaseOrderItem query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<SrmPurchaseOrderItem>()
            .eq(query.getPurchaseOrderId() != null, SrmPurchaseOrderItem::getPurchaseOrderId, query.getPurchaseOrderId())
            .eq(query.getPurchaseRequestId() != null, SrmPurchaseOrderItem::getPurchaseRequestId, query.getPurchaseRequestId())
            .eq(query.getWorkOrderId() != null, SrmPurchaseOrderItem::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, SrmPurchaseOrderItem::getContractId, query.getContractId())
            .eq(query.getPartId() != null, SrmPurchaseOrderItem::getPartId, query.getPartId())
            .like(StringUtils.isNotBlank(query.getPartCode()), SrmPurchaseOrderItem::getPartCode, query.getPartCode())
            .like(StringUtils.isNotBlank(query.getPartName()), SrmPurchaseOrderItem::getPartName, query.getPartName())
            .eq(StringUtils.isNotBlank(query.getSpecification()), SrmPurchaseOrderItem::getSpecification, query.getSpecification())
            .eq(StringUtils.isNotBlank(query.getUnit()), SrmPurchaseOrderItem::getUnit, query.getUnit())
            .eq(StringUtils.isNotBlank(query.getStatus()), SrmPurchaseOrderItem::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), SrmPurchaseOrderItem::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, SrmPurchaseOrderItem::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(SrmPurchaseOrderItem::getCreateTime);
    }
}
