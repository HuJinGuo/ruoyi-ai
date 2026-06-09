package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.crm.CrmOpportunity;
import org.ruoyi.system.domain.crm.CrmQuote;
import org.ruoyi.system.domain.crm.bo.CrmContractBo;
import org.ruoyi.system.domain.crm.vo.CrmContractVo;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.crm.CrmOpportunityMapper;
import org.ruoyi.system.mapper.crm.CrmQuoteMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRM 合同服务
 */
@Service
public class CrmContractService extends CrmCrudService<CrmContract, CrmContractVo, CrmContractBo> {

    private final CrmCustomerMapper customerMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final CrmQuoteMapper quoteMapper;

    public CrmContractService(CrmContractMapper contractMapper, CrmCustomerMapper customerMapper,
                              CrmOpportunityMapper opportunityMapper, CrmQuoteMapper quoteMapper) {
        super(contractMapper);
        this.customerMapper = customerMapper;
        this.opportunityMapper = opportunityMapper;
        this.quoteMapper = quoteMapper;
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

    @Override
    protected void fillDisplayFields(List<CrmContractVo> records) {
        Set<Long> customerIds = CrmDisplayFields.ids(records, CrmContractVo::getCustomerId);
        Set<Long> opportunityIds = CrmDisplayFields.ids(records, CrmContractVo::getOpportunityId);
        Set<Long> quoteIds = CrmDisplayFields.ids(records, CrmContractVo::getQuoteId);
        Map<Long, CrmCustomer> customers = CrmDisplayFields.customerMap(customerMapper, customerIds);
        Map<Long, CrmOpportunity> opportunities = CrmDisplayFields.opportunityMap(opportunityMapper, opportunityIds);
        Map<Long, CrmQuote> quotes = CrmDisplayFields.quoteMap(quoteMapper, quoteIds);
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
            CrmQuote quote = quotes.get(record.getQuoteId());
            if (quote != null) {
                record.setQuoteName(CrmDisplayFields.quoteName(quote));
            }
        });
    }
}
