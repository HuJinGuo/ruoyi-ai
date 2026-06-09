package org.ruoyi.system.domain.srm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import java.util.Date;

/**
 * XTP 采购订单表 srm_purchase_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("srm_purchase_order")
public class SrmPurchaseOrder extends TenantEntity {

    @TableId(value = "purchase_order_id")
    private Long purchaseOrderId;

    private String purchaseOrderCode;

    private Long purchaseRequestId;

    @TableField(exist = false)
    private String purchaseRequestName;

    private Long supplierId;

    @TableField(exist = false)
    private String supplierCode;

    @TableField(exist = false)
    private String supplierName;

    private Long workOrderId;

    @TableField(exist = false)
    private String workOrderCode;

    @TableField(exist = false)
    private String projectName;

    private Long contractId;

    @TableField(exist = false)
    private String contractName;

    private String status;

    private Date orderDate;

    private Date expectedDeliveryDate;

    private String remark;

}
