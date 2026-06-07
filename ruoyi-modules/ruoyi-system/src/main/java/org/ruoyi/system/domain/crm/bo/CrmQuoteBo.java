package org.ruoyi.system.domain.crm.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.crm.CrmQuote;

import java.math.BigDecimal;

/**
 * CRM 报价业务对象 crm_quote
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CrmQuote.class, reverseConvertGenerate = false)
public class CrmQuoteBo extends BaseEntity {

    private Long quoteId;

    private Long opportunityId;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    private Integer version;

    private BigDecimal totalAmount;

    private String status;

    private String remark;
}
