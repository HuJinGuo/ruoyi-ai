package org.ruoyi.system.service.mes;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MesWorkOrderService extends XtpCrudService<MesWorkOrder> {

    public MesWorkOrderService(MesWorkOrderMapper mapper) {
        super(mapper);
    }

    @Override
    protected Wrapper<MesWorkOrder> buildQueryWrapper(MesWorkOrder query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<MesWorkOrder>()
            .eq(query.getContractId() != null, MesWorkOrder::getContractId, query.getContractId())
            .eq(query.getCustomerId() != null, MesWorkOrder::getCustomerId, query.getCustomerId())
            .eq(query.getOpportunityId() != null, MesWorkOrder::getOpportunityId, query.getOpportunityId())
            .like(StringUtils.isNotBlank(query.getWorkOrderCode()), MesWorkOrder::getWorkOrderCode, query.getWorkOrderCode())
            .like(StringUtils.isNotBlank(query.getProjectName()), MesWorkOrder::getProjectName, query.getProjectName())
            .like(StringUtils.isNotBlank(query.getProductName()), MesWorkOrder::getProductName, query.getProductName())
            .eq(StringUtils.isNotBlank(query.getCurrentStage()), MesWorkOrder::getCurrentStage, query.getCurrentStage())
            .eq(StringUtils.isNotBlank(query.getStatus()), MesWorkOrder::getStatus, query.getStatus())
            .eq(query.getResponsibleUserId() != null, MesWorkOrder::getResponsibleUserId, query.getResponsibleUserId())
            .eq(StringUtils.isNotBlank(query.getRemark()), MesWorkOrder::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, MesWorkOrder::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(MesWorkOrder::getCreateTime);
    }
}
