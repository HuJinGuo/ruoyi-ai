package org.ruoyi.system.domain.mes;

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

    private Long customerId;

    private Long opportunityId;

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

    private String remark;

}
