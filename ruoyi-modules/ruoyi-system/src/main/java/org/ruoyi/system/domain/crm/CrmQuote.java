package org.ruoyi.system.domain.crm;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * CRM 报价表 crm_quote
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_quote")
public class CrmQuote extends TenantEntity {

    @TableId(value = "quote_id")
    private Long quoteId;

    private Long opportunityId;

    private Long customerId;

    private Integer version;

    private BigDecimal totalAmount;

    private String status;

    private String remark;
}
