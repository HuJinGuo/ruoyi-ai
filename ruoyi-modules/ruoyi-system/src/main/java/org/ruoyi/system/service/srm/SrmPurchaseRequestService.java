package org.ruoyi.system.service.srm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.srm.SrmPurchaseRequest;
import org.ruoyi.system.mapper.srm.SrmPurchaseRequestMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SrmPurchaseRequestService extends XtpCrudService<SrmPurchaseRequest> {

    public SrmPurchaseRequestService(SrmPurchaseRequestMapper mapper) {
        super(mapper);
    }

    @Override
    protected Wrapper<SrmPurchaseRequest> buildQueryWrapper(SrmPurchaseRequest query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<SrmPurchaseRequest>()
            .eq(query.getWorkOrderId() != null, SrmPurchaseRequest::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, SrmPurchaseRequest::getContractId, query.getContractId())
            .eq(query.getEngineeringMaterialId() != null, SrmPurchaseRequest::getEngineeringMaterialId, query.getEngineeringMaterialId())
            .eq(query.getSupplierId() != null, SrmPurchaseRequest::getSupplierId, query.getSupplierId())
            .eq(query.getPartId() != null, SrmPurchaseRequest::getPartId, query.getPartId())
            .like(StringUtils.isNotBlank(query.getPartCode()), SrmPurchaseRequest::getPartCode, query.getPartCode())
            .like(StringUtils.isNotBlank(query.getPartName()), SrmPurchaseRequest::getPartName, query.getPartName())
            .eq(StringUtils.isNotBlank(query.getSpecification()), SrmPurchaseRequest::getSpecification, query.getSpecification())
            .eq(StringUtils.isNotBlank(query.getUnit()), SrmPurchaseRequest::getUnit, query.getUnit())
            .eq(StringUtils.isNotBlank(query.getStatus()), SrmPurchaseRequest::getStatus, query.getStatus())
            .eq(StringUtils.isNotBlank(query.getRemark()), SrmPurchaseRequest::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, SrmPurchaseRequest::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(SrmPurchaseRequest::getCreateTime);
    }
}
