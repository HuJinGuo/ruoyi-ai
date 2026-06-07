package org.ruoyi.system.domain.crm.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.crm.CrmContract;

import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 合同业务对象 crm_contract
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CrmContract.class, reverseConvertGenerate = false)
public class CrmContractBo extends BaseEntity {

    private Long contractId;

    private Long opportunityId;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    private Long quoteId;

    @NotBlank(message = "合同名称不能为空")
    private String name;

    private BigDecimal amount;

    private Date signedDate;

    private Date deliveryDate;

    private String status;

    private String remark;
}
