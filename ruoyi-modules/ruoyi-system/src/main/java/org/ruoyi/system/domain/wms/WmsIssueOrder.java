package org.ruoyi.system.domain.wms;

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

    private Long contractId;

    private String issueStatus;

    private Date issueTime;

    private Long warehouseUserId;

    private String remark;

}
