package org.ruoyi.system.domain.wms;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import java.math.BigDecimal;

/**
 * XTP 收料单明细表 wms_receipt_order_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_receipt_order_item")
public class WmsReceiptOrderItem extends TenantEntity {

    @TableId(value = "receipt_order_item_id")
    private Long receiptOrderItemId;

    private Long receiptOrderId;

    @TableField(exist = false)
    private String receiptOrderName;

    private Long purchaseOrderId;

    @TableField(exist = false)
    private String purchaseOrderCode;

    private Long purchaseOrderItemId;

    @TableField(exist = false)
    private String purchaseOrderItemName;

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

    private BigDecimal receiptQty;

    private String status;

    private String remark;

}
