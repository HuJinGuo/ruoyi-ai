package org.ruoyi.system.domain.material;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

/**
 * XTP 物料表 material_part
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material_part")
public class MaterialPart extends TenantEntity {

    @TableId(value = "part_id")
    private Long partId;

    private String partCode;

    private String partName;

    private String specification;

    private String unit;

    private String material;

    private String category;

    private Long defaultSupplierId;

    @TableField(exist = false)
    private String defaultSupplierCode;

    @TableField(exist = false)
    private String defaultSupplierName;

    private String status;

    private String remark;

}
