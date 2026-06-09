package org.ruoyi.system.domain.srm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import java.math.BigDecimal;

/**
 * XTP 采购订单明细表 srm_purchase_order_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("srm_purchase_order_item")
public class SrmPurchaseOrderItem extends TenantEntity {

    @TableId(value = "purchase_order_item_id")
    private Long purchaseOrderItemId;

    private Long purchaseOrderId;

    @TableField(exist = false)
    private String purchaseOrderCode;

    private Long purchaseRequestId;

    @TableField(exist = false)
    private String purchaseRequestName;

    private Long workOrderId;

    @TableField(exist = false)
    private String workOrderCode;

    @TableField(exist = false)
    private String projectName;

    private Long contractId;

    @TableField(exist = false)
    private String contractName;

    private Long partId;

    private String partCode;

    private String partName;

    private String specification;

    private String unit;

    private BigDecimal purchaseQty;

    private BigDecimal price;

    private BigDecimal amount;

    private BigDecimal receivedQty;

    private String status;

    private String remark;

}
