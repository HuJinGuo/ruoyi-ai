package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmContact;
import org.ruoyi.system.domain.crm.bo.CrmContactBo;
import org.ruoyi.system.domain.crm.vo.CrmContactVo;
import org.ruoyi.system.mapper.crm.CrmContactMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * CRM 联系人服务
 */
@Service
public class CrmContactService extends CrmCrudService<CrmContact, CrmContactVo, CrmContactBo> {

    public CrmContactService(CrmContactMapper contactMapper) {
        super(contactMapper);
    }

    @Override
    protected Wrapper<CrmContact> buildQueryWrapper(CrmContactBo bo) {
        Map<String, Object> params = bo.getParams();
        return new LambdaQueryWrapper<CrmContact>()
            .eq(bo.getCustomerId() != null, CrmContact::getCustomerId, bo.getCustomerId())
            .like(StringUtils.isNotBlank(bo.getName()), CrmContact::getName, bo.getName())
            .like(StringUtils.isNotBlank(bo.getPhone()), CrmContact::getPhone, bo.getPhone())
            .like(StringUtils.isNotBlank(bo.getPosition()), CrmContact::getPosition, bo.getPosition())
            .eq(StringUtils.isNotBlank(bo.getDecisionRole()), CrmContact::getDecisionRole, bo.getDecisionRole())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                CrmContact::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(CrmContact::getCreateTime);
    }

    @Override
    protected Class<CrmContact> getEntityClass() {
        return CrmContact.class;
    }
}
