package org.ruoyi.system.service.wms;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.wms.WmsIssueOrder;
import org.ruoyi.system.mapper.wms.WmsIssueOrderMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WmsIssueOrderService extends XtpCrudService<WmsIssueOrder> {

    public WmsIssueOrderService(WmsIssueOrderMapper mapper) {
        super(mapper);
    }

    @Override
    protected Wrapper<WmsIssueOrder> buildQueryWrapper(WmsIssueOrder query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<WmsIssueOrder>()
            .eq(query.getWorkOrderId() != null, WmsIssueOrder::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, WmsIssueOrder::getContractId, query.getContractId())
            .eq(StringUtils.isNotBlank(query.getIssueStatus()), WmsIssueOrder::getIssueStatus, query.getIssueStatus())
            .eq(query.getWarehouseUserId() != null, WmsIssueOrder::getWarehouseUserId, query.getWarehouseUserId())
            .eq(StringUtils.isNotBlank(query.getRemark()), WmsIssueOrder::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, WmsIssueOrder::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(WmsIssueOrder::getCreateTime);
    }
}
