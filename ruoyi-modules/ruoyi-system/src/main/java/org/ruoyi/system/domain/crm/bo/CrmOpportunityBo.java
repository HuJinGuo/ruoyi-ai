package org.ruoyi.system.domain.crm.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.crm.CrmOpportunity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 商机业务对象 crm_opportunity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CrmOpportunity.class, reverseConvertGenerate = false)
public class CrmOpportunityBo extends BaseEntity {

    private Long opportunityId;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    private Long contactId;

    @NotBlank(message = "商机名称不能为空")
    private String name;

    private BigDecimal estimatedAmount;

    private Date estimatedCloseDate;

    private String source;

    private String stage;

    private BigDecimal successRate;

    private String remark;
}
