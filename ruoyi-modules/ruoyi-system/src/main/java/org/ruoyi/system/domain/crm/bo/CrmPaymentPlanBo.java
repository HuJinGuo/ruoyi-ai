package org.ruoyi.system.domain.crm.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.crm.CrmPaymentPlan;

import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 回款计划业务对象 crm_payment_plan
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CrmPaymentPlan.class, reverseConvertGenerate = false)
public class CrmPaymentPlanBo extends BaseEntity {

    private Long paymentId;

    private Long contractId;

    private Long opportunityId;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotBlank(message = "付款节点不能为空")
    private String stageName;

    private BigDecimal amount;

    private Date plannedDate;

    private String status;

    private String remark;
}
