package org.ruoyi.system.domain.wms;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import java.util.Date;

/**
 * XTP 发料单表 wms_issue_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_issue_order")
public class WmsIssueOrder extends TenantEntity {

    @TableId(value = "issue_order_id")
    private Long issueOrderId;

    private Long workOrderId;

    @TableField(exist = false)
    private String workOrderCode;

    @TableField(exist = false)
    private String projectName;

    private Long contractId;

    @TableField(exist = false)
    private String contractName;

    private String issueStatus;

    private Date issueTime;

    private Long warehouseUserId;

    @TableField(exist = false)
    private String warehouseUserName;

    private String remark;

}
