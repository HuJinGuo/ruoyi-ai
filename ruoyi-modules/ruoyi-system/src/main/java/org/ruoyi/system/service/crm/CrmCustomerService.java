package org.ruoyi.system.service.crm;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.crm.bo.CrmCustomerBo;
import org.ruoyi.system.domain.crm.vo.CrmCustomerVo;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * CRM 客户服务
 */
@Service
public class CrmCustomerService extends CrmCrudService<CrmCustomer, CrmCustomerVo, CrmCustomerBo> {

    private final CrmCustomerMapper customerMapper;

    public CrmCustomerService(CrmCustomerMapper customerMapper) {
        super(customerMapper);
        this.customerMapper = customerMapper;
    }

    @Override
    protected Wrapper<CrmCustomer> buildQueryWrapper(CrmCustomerBo bo) {
        Map<String, Object> params = bo.getParams();
        return new LambdaQueryWrapper<CrmCustomer>()
            .like(StringUtils.isNotBlank(bo.getName()), CrmCustomer::getName, bo.getName())
            .like(StringUtils.isNotBlank(bo.getShortName()), CrmCustomer::getShortName, bo.getShortName())
            .like(StringUtils.isNotBlank(bo.getCode()), CrmCustomer::getCode, bo.getCode())
            .eq(StringUtils.isNotBlank(bo.getType()), CrmCustomer::getType, bo.getType())
            .eq(StringUtils.isNotBlank(bo.getLevel()), CrmCustomer::getLevel, bo.getLevel())
            .eq(StringUtils.isNotBlank(bo.getStatus()), CrmCustomer::getStatus, bo.getStatus())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                CrmCustomer::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(CrmCustomer::getCreateTime);
    }

    @Override
    protected Class<CrmCustomer> getEntityClass() {
        return CrmCustomer.class;
    }

    public boolean checkCodeUnique(CrmCustomerBo bo) {
        boolean exist = customerMapper.exists(new LambdaQueryWrapper<CrmCustomer>()
            .eq(CrmCustomer::getCode, bo.getCode())
            .ne(ObjectUtil.isNotNull(bo.getCustomerId()), CrmCustomer::getCustomerId, bo.getCustomerId()));
        return !exist;
    }
}
