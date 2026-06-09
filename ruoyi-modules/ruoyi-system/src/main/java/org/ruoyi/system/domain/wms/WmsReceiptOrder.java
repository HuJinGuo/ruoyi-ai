package org.ruoyi.system.domain.wms;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import java.util.Date;

/**
 * XTP 收料单表 wms_receipt_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_receipt_order")
public class WmsReceiptOrder extends TenantEntity {

    @TableId(value = "receipt_order_id")
    private Long receiptOrderId;

    private Long purchaseOrderId;

    @TableField(exist = false)
    private String purchaseOrderCode;

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

    private String receiptStatus;

    private Date receiptTime;

    private Long warehouseUserId;

    @TableField(exist = false)
    private String warehouseUserName;

    private String remark;

}
