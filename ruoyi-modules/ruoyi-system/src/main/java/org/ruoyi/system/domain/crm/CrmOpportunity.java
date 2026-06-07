package org.ruoyi.system.domain.crm;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 商机表 crm_opportunity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity")
public class CrmOpportunity extends TenantEntity {

    @TableId(value = "opportunity_id")
    private Long opportunityId;

    private Long customerId;

    private Long contactId;

    private String name;

    private BigDecimal estimatedAmount;

    private Date estimatedCloseDate;

    private String source;

    private String stage;

    private BigDecimal successRate;

    private String remark;
}
