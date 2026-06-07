package org.ruoyi.system.service.srm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.srm.SrmPurchaseOrder;
import org.ruoyi.system.mapper.srm.SrmPurchaseOrderMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SrmPurchaseOrderService extends XtpCrudService<SrmPurchaseOrder> {

    public SrmPurchaseOrderService(SrmPurchaseOrderMapper mapper) {
        super(mapper);
    }

    @Override
    protected Wrapper<SrmPurchaseOrder> buildQueryWrapper(SrmPurchaseOrder query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<SrmPurchaseOrder>()
            .like(StringUtils.isNotBlank(query.getPurchaseOrderCode()), SrmPurchaseOrder::getPurchaseOrderCode, query.getPurchaseOrderCode())
            .eq(query.getPurchaseRequestId() != null, SrmPurchaseOrder::getPurchaseRequestId, query.getPurchaseRequestId())
            .eq(query.getSupplierId() != null, SrmPurchaseOrder::getSupplierId, query.getSupplierId())
            .eq(query.getWorkOrderId() != null, SrmPurchaseOrder::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, SrmPurchaseOrder::getContractId, query.getContractId())
            .eq(StringUtils.isNotBlank(query.getStatus()), SrmPurchaseOrder::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), SrmPurchaseOrder::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, SrmPurchaseOrder::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(SrmPurchaseOrder::getCreateTime);
    }
}
