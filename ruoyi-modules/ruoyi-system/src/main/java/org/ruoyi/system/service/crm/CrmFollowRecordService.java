package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContact;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.crm.CrmFollowRecord;
import org.ruoyi.system.domain.crm.CrmOpportunity;
import org.ruoyi.system.domain.crm.bo.CrmFollowRecordBo;
import org.ruoyi.system.domain.crm.vo.CrmFollowRecordVo;
import org.ruoyi.system.mapper.crm.CrmContactMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.crm.CrmFollowRecordMapper;
import org.ruoyi.system.mapper.crm.CrmOpportunityMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRM 跟进记录服务
 */
@Service
public class CrmFollowRecordService extends CrmCrudService<CrmFollowRecord, CrmFollowRecordVo, CrmFollowRecordBo> {

    private final CrmCustomerMapper customerMapper;
    private final CrmContactMapper contactMapper;
    private final CrmOpportunityMapper opportunityMapper;

    public CrmFollowRecordService(CrmFollowRecordMapper followRecordMapper, CrmCustomerMapper customerMapper,
                                  CrmContactMapper contactMapper, CrmOpportunityMapper opportunityMapper) {
        super(followRecordMapper);
        this.customerMapper = customerMapper;
        this.contactMapper = contactMapper;
        this.opportunityMapper = opportunityMapper;
    }

    @Override
    protected Wrapper<CrmFollowRecord> buildQueryWrapper(CrmFollowRecordBo bo) {
        Map<String, Object> params = bo.getParams();
        return new LambdaQueryWrapper<CrmFollowRecord>()
            .eq(bo.getOpportunityId() != null, CrmFollowRecord::getOpportunityId, bo.getOpportunityId())
            .eq(bo.getCustomerId() != null, CrmFollowRecord::getCustomerId, bo.getCustomerId())
            .eq(bo.getContactId() != null, CrmFollowRecord::getContactId, bo.getContactId())
            .eq(StringUtils.isNotBlank(bo.getFollowMethod()), CrmFollowRecord::getFollowMethod, bo.getFollowMethod())
            .eq(StringUtils.isNotBlank(bo.getResult()), CrmFollowRecord::getResult, bo.getResult())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                CrmFollowRecord::getFollowTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(CrmFollowRecord::getFollowTime);
    }

    @Override
    protected Class<CrmFollowRecord> getEntityClass() {
        return CrmFollowRecord.class;
    }

    @Override
    protected void fillDisplayFields(List<CrmFollowRecordVo> records) {
        Set<Long> customerIds = CrmDisplayFields.ids(records, CrmFollowRecordVo::getCustomerId);
        Set<Long> contactIds = CrmDisplayFields.ids(records, CrmFollowRecordVo::getContactId);
        Set<Long> opportunityIds = CrmDisplayFields.ids(records, CrmFollowRecordVo::getOpportunityId);
        Map<Long, CrmCustomer> customers = CrmDisplayFields.customerMap(customerMapper, customerIds);
        Map<Long, CrmContact> contacts = CrmDisplayFields.contactMap(contactMapper, contactIds);
        Map<Long, CrmOpportunity> opportunities = CrmDisplayFields.opportunityMap(opportunityMapper, opportunityIds);
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
            CrmOpportunity opportunity = opportunities.get(record.getOpportunityId());
            if (opportunity != null) {
                record.setOpportunityName(opportunity.getName());
            }
        });
    }
}
