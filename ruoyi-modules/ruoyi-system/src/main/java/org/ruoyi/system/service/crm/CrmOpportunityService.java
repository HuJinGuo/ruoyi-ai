package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContact;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.crm.CrmOpportunity;
import org.ruoyi.system.domain.crm.bo.CrmOpportunityBo;
import org.ruoyi.system.domain.crm.vo.CrmOpportunityVo;
import org.ruoyi.system.mapper.crm.CrmContactMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.crm.CrmOpportunityMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRM 商机服务
 */
@Service
public class CrmOpportunityService extends CrmCrudService<CrmOpportunity, CrmOpportunityVo, CrmOpportunityBo> {

    private final CrmCustomerMapper customerMapper;
    private final CrmContactMapper contactMapper;

    public CrmOpportunityService(CrmOpportunityMapper opportunityMapper, CrmCustomerMapper customerMapper,
                                 CrmContactMapper contactMapper) {
        super(opportunityMapper);
        this.customerMapper = customerMapper;
        this.contactMapper = contactMapper;
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

    @Override
    protected void fillDisplayFields(List<CrmOpportunityVo> records) {
        Set<Long> customerIds = CrmDisplayFields.ids(records, CrmOpportunityVo::getCustomerId);
        Set<Long> contactIds = CrmDisplayFields.ids(records, CrmOpportunityVo::getContactId);
        Map<Long, CrmCustomer> customers = CrmDisplayFields.customerMap(customerMapper, customerIds);
        Map<Long, CrmContact> contacts = CrmDisplayFields.contactMap(contactMapper, contactIds);
        records.forEach(record -> {
            CrmCustomer customer = customers.get(record.getCustomerId());
            if (customer != null) {
                record.setCustomerCode(customer.getCode());
                record.setCustomerName(customer.getName());
            }
            CrmContact contact = contacts.get(record.getContactId());
            if (contact != null) {
                record.setContactName(contact.getName());
            }
        });
    }
}
