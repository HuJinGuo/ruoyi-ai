package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.crm.CrmOpportunity;
import org.ruoyi.system.domain.crm.CrmQuote;
import org.ruoyi.system.domain.crm.bo.CrmQuoteBo;
import org.ruoyi.system.domain.crm.vo.CrmQuoteVo;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.crm.CrmOpportunityMapper;
import org.ruoyi.system.mapper.crm.CrmQuoteMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRM 报价服务
 */
@Service
public class CrmQuoteService extends CrmCrudService<CrmQuote, CrmQuoteVo, CrmQuoteBo> {

    private final CrmCustomerMapper customerMapper;
    private final CrmOpportunityMapper opportunityMapper;

    public CrmQuoteService(CrmQuoteMapper quoteMapper, CrmCustomerMapper customerMapper,
                           CrmOpportunityMapper opportunityMapper) {
        super(quoteMapper);
        this.customerMapper = customerMapper;
        this.opportunityMapper = opportunityMapper;
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

    @Override
    protected void fillDisplayFields(List<CrmQuoteVo> records) {
        Set<Long> customerIds = CrmDisplayFields.ids(records, CrmQuoteVo::getCustomerId);
        Set<Long> opportunityIds = CrmDisplayFields.ids(records, CrmQuoteVo::getOpportunityId);
        Map<Long, CrmCustomer> customers = CrmDisplayFields.customerMap(customerMapper, customerIds);
        Map<Long, CrmOpportunity> opportunities = CrmDisplayFields.opportunityMap(opportunityMapper, opportunityIds);
        records.forEach(record -> {
            CrmCustomer customer = customers.get(record.getCustomerId());
            if (customer != null) {
                record.setCustomerCode(customer.getCode());
                record.setCustomerName(customer.getName());
            }
            CrmOpportunity opportunity = opportunities.get(record.getOpportunityId());
            if (opportunity != null) {
                record.setOpportunityName(opportunity.getName());
            }
        });
    }
}
