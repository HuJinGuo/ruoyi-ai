package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmQuote;
import org.ruoyi.system.domain.crm.bo.CrmQuoteBo;
import org.ruoyi.system.domain.crm.vo.CrmQuoteVo;
import org.ruoyi.system.mapper.crm.CrmQuoteMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * CRM 报价服务
 */
@Service
public class CrmQuoteService extends CrmCrudService<CrmQuote, CrmQuoteVo, CrmQuoteBo> {

    public CrmQuoteService(CrmQuoteMapper quoteMapper) {
        super(quoteMapper);
    }

    @Override
    protected Wrapper<CrmQuote> buildQueryWrapper(CrmQuoteBo bo) {
        Map<String, Object> params = bo.getParams();
        return new LambdaQueryWrapper<CrmQuote>()
            .eq(bo.getOpportunityId() != null, CrmQuote::getOpportunityId, bo.getOpportunityId())
            .eq(bo.getCustomerId() != null, CrmQuote::getCustomerId, bo.getCustomerId())
            .eq(StringUtils.isNotBlank(bo.getStatus()), CrmQuote::getStatus, bo.getStatus())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                CrmQuote::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(CrmQuote::getCreateTime);
    }

    @Override
    protected Class<CrmQuote> getEntityClass() {
        return CrmQuote.class;
    }
}
