package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.crm.bo.CrmContractBo;
import org.ruoyi.system.domain.crm.vo.CrmContractVo;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * CRM 合同服务
 */
@Service
public class CrmContractService extends CrmCrudService<CrmContract, CrmContractVo, CrmContractBo> {

    public CrmContractService(CrmContractMapper contractMapper) {
        super(contractMapper);
    }

    @Override
    protected Wrapper<CrmContract> buildQueryWrapper(CrmContractBo bo) {
        Map<String, Object> params = bo.getParams();
        return new LambdaQueryWrapper<CrmContract>()
            .eq(bo.getOpportunityId() != null, CrmContract::getOpportunityId, bo.getOpportunityId())
            .eq(bo.getCustomerId() != null, CrmContract::getCustomerId, bo.getCustomerId())
            .eq(bo.getQuoteId() != null, CrmContract::getQuoteId, bo.getQuoteId())
            .like(StringUtils.isNotBlank(bo.getName()), CrmContract::getName, bo.getName())
            .eq(StringUtils.isNotBlank(bo.getStatus()), CrmContract::getStatus, bo.getStatus())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                CrmContract::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(CrmContract::getCreateTime);
    }

    @Override
    protected Class<CrmContract> getEntityClass() {
        return CrmContract.class;
    }
}
