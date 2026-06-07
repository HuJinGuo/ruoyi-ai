package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmOpportunity;
import org.ruoyi.system.domain.crm.bo.CrmOpportunityBo;
import org.ruoyi.system.domain.crm.vo.CrmOpportunityVo;
import org.ruoyi.system.mapper.crm.CrmOpportunityMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * CRM 商机服务
 */
@Service
public class CrmOpportunityService extends CrmCrudService<CrmOpportunity, CrmOpportunityVo, CrmOpportunityBo> {

    public CrmOpportunityService(CrmOpportunityMapper opportunityMapper) {
        super(opportunityMapper);
    }

    @Override
    protected Wrapper<CrmOpportunity> buildQueryWrapper(CrmOpportunityBo bo) {
        Map<String, Object> params = bo.getParams();
        return new LambdaQueryWrapper<CrmOpportunity>()
            .eq(bo.getCustomerId() != null, CrmOpportunity::getCustomerId, bo.getCustomerId())
            .eq(bo.getContactId() != null, CrmOpportunity::getContactId, bo.getContactId())
            .like(StringUtils.isNotBlank(bo.getName()), CrmOpportunity::getName, bo.getName())
            .eq(StringUtils.isNotBlank(bo.getSource()), CrmOpportunity::getSource, bo.getSource())
            .eq(StringUtils.isNotBlank(bo.getStage()), CrmOpportunity::getStage, bo.getStage())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                CrmOpportunity::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(CrmOpportunity::getCreateTime);
    }

    @Override
    protected Class<CrmOpportunity> getEntityClass() {
        return CrmOpportunity.class;
    }
}
