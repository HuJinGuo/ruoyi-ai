package org.ruoyi.system.domain.srm;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import java.math.BigDecimal;

/**
 * XTP 采购需求表 srm_purchase_request
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("srm_purchase_request")
public class SrmPurchaseRequest extends TenantEntity {

    @TableId(value = "purchase_request_id")
    private Long purchaseRequestId;

    private Long workOrderId;

    private Long contractId;

    private Long engineeringMaterialId;

    private Long supplierId;

    private Long partId;

    private String partCode;

    private String partName;

    private String specification;

    private String unit;

    private BigDecimal requestQty;

    private String status;

    private String remark;

}
