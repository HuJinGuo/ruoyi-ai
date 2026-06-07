package org.ruoyi.system.service.wms;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.wms.WmsReceiptOrderItem;
import org.ruoyi.system.mapper.wms.WmsReceiptOrderItemMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WmsReceiptOrderItemService extends XtpCrudService<WmsReceiptOrderItem> {

    public WmsReceiptOrderItemService(WmsReceiptOrderItemMapper mapper) {
        super(mapper);
    }

    @Override
    protected Wrapper<WmsReceiptOrderItem> buildQueryWrapper(WmsReceiptOrderItem query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<WmsReceiptOrderItem>()
            .eq(query.getReceiptOrderId() != null, WmsReceiptOrderItem::getReceiptOrderId, query.getReceiptOrderId())
            .eq(query.getPurchaseOrderId() != null, WmsReceiptOrderItem::getPurchaseOrderId, query.getPurchaseOrderId())
            .eq(query.getPurchaseOrderItemId() != null, WmsReceiptOrderItem::getPurchaseOrderItemId, query.getPurchaseOrderItemId())
            .eq(query.getWorkOrderId() != null, WmsReceiptOrderItem::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, WmsReceiptOrderItem::getContractId, query.getContractId())
            .eq(query.getPartId() != null, WmsReceiptOrderItem::getPartId, query.getPartId())
            .like(StringUtils.isNotBlank(query.getPartCode()), WmsReceiptOrderItem::getPartCode, query.getPartCode())
            .like(StringUtils.isNotBlank(query.getPartName()), WmsReceiptOrderItem::getPartName, query.getPartName())
            .eq(StringUtils.isNotBlank(query.getSpecification()), WmsReceiptOrderItem::getSpecification, query.getSpecification())
            .eq(StringUtils.isNotBlank(query.getUnit()), WmsReceiptOrderItem::getUnit, query.getUnit())
            .eq(StringUtils.isNotBlank(query.getStatus()), WmsReceiptOrderItem::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), WmsReceiptOrderItem::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, WmsReceiptOrderItem::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(WmsReceiptOrderItem::getCreateTime);
    }
}
