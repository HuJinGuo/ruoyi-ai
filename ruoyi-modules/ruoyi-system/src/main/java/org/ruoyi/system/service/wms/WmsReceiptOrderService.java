package org.ruoyi.system.service.wms;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.wms.WmsReceiptOrder;
import org.ruoyi.system.mapper.wms.WmsReceiptOrderMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WmsReceiptOrderService extends XtpCrudService<WmsReceiptOrder> {

    public WmsReceiptOrderService(WmsReceiptOrderMapper mapper) {
        super(mapper);
    }

    @Override
    protected Wrapper<WmsReceiptOrder> buildQueryWrapper(WmsReceiptOrder query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<WmsReceiptOrder>()
            .eq(query.getPurchaseOrderId() != null, WmsReceiptOrder::getPurchaseOrderId, query.getPurchaseOrderId())
            .eq(query.getSupplierId() != null, WmsReceiptOrder::getSupplierId, query.getSupplierId())
            .eq(query.getWorkOrderId() != null, WmsReceiptOrder::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, WmsReceiptOrder::getContractId, query.getContractId())
            .eq(StringUtils.isNotBlank(query.getReceiptStatus()), WmsReceiptOrder::getReceiptStatus, query.getReceiptStatus())
            .eq(query.getWarehouseUserId() != null, WmsReceiptOrder::getWarehouseUserId, query.getWarehouseUserId())
            .eq(StringUtils.isNotBlank(query.getRemark()), WmsReceiptOrder::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, WmsReceiptOrder::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(WmsReceiptOrder::getCreateTime);
    }
}
