package org.ruoyi.system.service.mes;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.mes.MesWorkOrderStage;
import org.ruoyi.system.mapper.mes.MesWorkOrderStageMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MesWorkOrderStageService extends XtpCrudService<MesWorkOrderStage> {

    public MesWorkOrderStageService(MesWorkOrderStageMapper mapper) {
        super(mapper);
    }

    @Override
    protected Wrapper<MesWorkOrderStage> buildQueryWrapper(MesWorkOrderStage query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<MesWorkOrderStage>()
            .eq(query.getWorkOrderId() != null, MesWorkOrderStage::getWorkOrderId, query.getWorkOrderId())
            .like(StringUtils.isNotBlank(query.getStageCode()), MesWorkOrderStage::getStageCode, query.getStageCode())
            .like(StringUtils.isNotBlank(query.getStageName()), MesWorkOrderStage::getStageName, query.getStageName())
            .eq(StringUtils.isNotBlank(query.getStatus()), MesWorkOrderStage::getStatus, query.getStatus())
            .eq(query.getResponsibleUserId() != null, MesWorkOrderStage::getResponsibleUserId, query.getResponsibleUserId())
            .eq(StringUtils.isNotBlank(query.getRemark()), MesWorkOrderStage::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, MesWorkOrderStage::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(MesWorkOrderStage::getCreateTime);
    }
}
