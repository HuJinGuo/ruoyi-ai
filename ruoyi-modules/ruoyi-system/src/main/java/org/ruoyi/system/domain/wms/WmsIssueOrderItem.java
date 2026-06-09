package org.ruoyi.system.domain.wms;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import java.math.BigDecimal;

/**
 * XTP 发料单明细表 wms_issue_order_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_issue_order_item")
public class WmsIssueOrderItem extends TenantEntity {

    @TableId(value = "issue_order_item_id")
    private Long issueOrderItemId;

    private Long issueOrderId;

    @TableField(exist = false)
    private String issueOrderName;

    private Long workOrderId;

    @TableField(exist = false)
    private String workOrderCode;

    @TableField(exist = false)
    private String projectName;

    private Long contractId;

    @TableField(exist = false)
    private String contractName;

    private Long engineeringMaterialId;

    @TableField(exist = false)
    private String engineeringMaterialName;

    private Long partId;

    private String partCode;

    private String partName;

    private String specification;

    private String unit;

    private BigDecimal issueQty;

    private String status;

    private String remark;

}
