package org.ruoyi.system.domain.engineering;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import java.math.BigDecimal;

/**
 * XTP 工程物料表 engineering_material
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("engineering_material")
public class EngineeringMaterial extends TenantEntity {

    @TableId(value = "engineering_material_id")
    private Long engineeringMaterialId;

    private Long workOrderId;

    private Long contractId;

    private Long partId;

    private String partCode;

    private String partName;

    private String specification;

    private String unit;

    private BigDecimal requiredQty;

    private BigDecimal stockQty;

    private BigDecimal shortageQty;

    private BigDecimal purchaseQty;

    private String status;

    private String remark;

}
