package org.ruoyi.system.domain.wms;

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

    private Long supplierId;

    private Long workOrderId;

    private Long contractId;

    private String receiptStatus;

    private Date receiptTime;

    private Long warehouseUserId;

    private String remark;

}
