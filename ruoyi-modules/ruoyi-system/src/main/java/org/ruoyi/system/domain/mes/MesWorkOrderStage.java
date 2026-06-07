package org.ruoyi.system.domain.mes;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import java.util.Date;

/**
 * XTP 工单阶段表 mes_work_order_stage
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_work_order_stage")
public class MesWorkOrderStage extends TenantEntity {

    @TableId(value = "work_order_stage_id")
    private Long workOrderStageId;

    private Long workOrderId;

    private String stageCode;

    private String stageName;

    private String status;

    private Long responsibleUserId;

    private Date startTime;

    private Date endTime;

    private String remark;

}
