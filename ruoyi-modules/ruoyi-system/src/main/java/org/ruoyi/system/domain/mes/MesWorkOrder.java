package org.ruoyi.system.domain.mes;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import java.math.BigDecimal;
import java.util.Date;

/**
 * XTP 生产工单表 mes_work_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_work_order")
public class MesWorkOrder extends TenantEntity {

    @TableId(value = "work_order_id")
    private Long workOrderId;

    private Long contractId;

    @TableField(exist = false)
    private String contractName;

    private Long customerId;

    @TableField(exist = false)
    private String customerCode;

    @TableField(exist = false)
    private String customerName;

    private Long opportunityId;

    @TableField(exist = false)
    private String opportunityName;

    private String workOrderCode;

    private String projectName;

    private String productName;

    private Integer quantity;

    private String currentStage;

    private BigDecimal progress;

    private String status;

    private Date planDeliveryDate;

    private Date actualDeliveryDate;

    private Long responsibleUserId;

    @TableField(exist = false)
    private String responsibleUserName;

    private String remark;

}
