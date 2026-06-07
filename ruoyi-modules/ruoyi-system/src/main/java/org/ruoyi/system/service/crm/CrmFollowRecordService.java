package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmFollowRecord;
import org.ruoyi.system.domain.crm.bo.CrmFollowRecordBo;
import org.ruoyi.system.domain.crm.vo.CrmFollowRecordVo;
import org.ruoyi.system.mapper.crm.CrmFollowRecordMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * CRM 跟进记录服务
 */
@Service
public class CrmFollowRecordService extends CrmCrudService<CrmFollowRecord, CrmFollowRecordVo, CrmFollowRecordBo> {

    public CrmFollowRecordService(CrmFollowRecordMapper followRecordMapper) {
        super(followRecordMapper);
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
}
