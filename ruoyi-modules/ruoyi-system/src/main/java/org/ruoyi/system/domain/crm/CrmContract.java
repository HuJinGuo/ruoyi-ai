package org.ruoyi.system.domain.crm;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 合同表 crm_contract
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_contract")
public class CrmContract extends TenantEntity {

    @TableId(value = "contract_id")
    private Long contractId;

    private Long opportunityId;

    private Long customerId;

    private Long quoteId;

    private String name;

    private BigDecimal amount;

    private Date signedDate;

    private Date deliveryDate;

    private String status;

    private String remark;
}
