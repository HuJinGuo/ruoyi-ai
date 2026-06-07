package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmPaymentPlan;
import org.ruoyi.system.domain.crm.bo.CrmPaymentPlanBo;
import org.ruoyi.system.domain.crm.vo.CrmPaymentPlanVo;
import org.ruoyi.system.mapper.crm.CrmPaymentPlanMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * CRM 回款计划服务
 */
@Service
public class CrmPaymentPlanService extends CrmCrudService<CrmPaymentPlan, CrmPaymentPlanVo, CrmPaymentPlanBo> {

    public CrmPaymentPlanService(CrmPaymentPlanMapper paymentPlanMapper) {
        super(paymentPlanMapper);
    }

    @Override
    protected Wrapper<CrmPaymentPlan> buildQueryWrapper(CrmPaymentPlanBo bo) {
        Map<String, Object> params = bo.getParams();
        return new LambdaQueryWrapper<CrmPaymentPlan>()
            .eq(bo.getContractId() != null, CrmPaymentPlan::getContractId, bo.getContractId())
            .eq(bo.getOpportunityId() != null, CrmPaymentPlan::getOpportunityId, bo.getOpportunityId())
            .eq(bo.getCustomerId() != null, CrmPaymentPlan::getCustomerId, bo.getCustomerId())
            .like(StringUtils.isNotBlank(bo.getStageName()), CrmPaymentPlan::getStageName, bo.getStageName())
            .eq(StringUtils.isNotBlank(bo.getStatus()), CrmPaymentPlan::getStatus, bo.getStatus())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                CrmPaymentPlan::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(CrmPaymentPlan::getCreateTime);
    }

    @Override
    protected Class<CrmPaymentPlan> getEntityClass() {
        return CrmPaymentPlan.class;
    }
}
