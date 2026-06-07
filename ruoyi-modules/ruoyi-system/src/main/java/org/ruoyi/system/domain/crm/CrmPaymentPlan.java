package org.ruoyi.system.domain.crm;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 回款计划表 crm_payment_plan
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_payment_plan")
public class CrmPaymentPlan extends TenantEntity {

    @TableId(value = "payment_id")
    private Long paymentId;

    private Long contractId;

    private Long opportunityId;

    private Long customerId;

    private String stageName;

    private BigDecimal amount;

    private Date plannedDate;

    private String status;

    private String remark;
}
