package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.crm.CrmOpportunity;
import org.ruoyi.system.domain.crm.CrmPaymentPlan;
import org.ruoyi.system.domain.crm.bo.CrmPaymentPlanBo;
import org.ruoyi.system.domain.crm.vo.CrmPaymentPlanVo;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.crm.CrmOpportunityMapper;
import org.ruoyi.system.mapper.crm.CrmPaymentPlanMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRM 回款计划服务
 */
@Service
public class CrmPaymentPlanService extends CrmCrudService<CrmPaymentPlan, CrmPaymentPlanVo, CrmPaymentPlanBo> {

    private final CrmContractMapper contractMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmOpportunityMapper opportunityMapper;

    public CrmPaymentPlanService(CrmPaymentPlanMapper paymentPlanMapper, CrmContractMapper contractMapper,
                                 CrmCustomerMapper customerMapper, CrmOpportunityMapper opportunityMapper) {
        super(paymentPlanMapper);
        this.contractMapper = contractMapper;
        this.customerMapper = customerMapper;
        this.opportunityMapper = opportunityMapper;
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

    @Override
    protected void fillDisplayFields(List<CrmPaymentPlanVo> records) {
        Set<Long> contractIds = CrmDisplayFields.ids(records, CrmPaymentPlanVo::getContractId);
        Set<Long> customerIds = CrmDisplayFields.ids(records, CrmPaymentPlanVo::getCustomerId);
        Set<Long> opportunityIds = CrmDisplayFields.ids(records, CrmPaymentPlanVo::getOpportunityId);
        Map<Long, CrmContract> contracts = CrmDisplayFields.contractMap(contractMapper, contractIds);
        Map<Long, CrmCustomer> customers = CrmDisplayFields.customerMap(customerMapper, customerIds);
        Map<Long, CrmOpportunity> opportunities = CrmDisplayFields.opportunityMap(opportunityMapper, opportunityIds);
        records.forEach(record -> {
            CrmContract contract = contracts.get(record.getContractId());
            if (contract != null) {
                record.setContractName(contract.getName());
            }
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
