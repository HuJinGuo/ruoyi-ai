package org.ruoyi.system.service.srm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.srm.SrmSupplier;
import org.ruoyi.system.mapper.srm.SrmSupplierMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SrmSupplierService extends XtpCrudService<SrmSupplier> {

    public SrmSupplierService(SrmSupplierMapper mapper) {
        super(mapper);
    }

    @Override
    protected Wrapper<SrmSupplier> buildQueryWrapper(SrmSupplier query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<SrmSupplier>()
            .like(StringUtils.isNotBlank(query.getSupplierCode()), SrmSupplier::getSupplierCode, query.getSupplierCode())
            .like(StringUtils.isNotBlank(query.getSupplierName()), SrmSupplier::getSupplierName, query.getSupplierName())
            .like(StringUtils.isNotBlank(query.getShortName()), SrmSupplier::getShortName, query.getShortName())
            .like(StringUtils.isNotBlank(query.getContactName()), SrmSupplier::getContactName, query.getContactName())
            .eq(StringUtils.isNotBlank(query.getPhone()), SrmSupplier::getPhone, query.getPhone())
            .eq(StringUtils.isNotBlank(query.getEmail()), SrmSupplier::getEmail, query.getEmail())
            .eq(StringUtils.isNotBlank(query.getAddress()), SrmSupplier::getAddress, query.getAddress())
            .eq(StringUtils.isNotBlank(query.getLevel()), SrmSupplier::getLevel, query.getLevel())
            .eq(StringUtils.isNotBlank(query.getStatus()), SrmSupplier::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), SrmSupplier::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, SrmSupplier::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(SrmSupplier::getCreateTime);
    }
}
