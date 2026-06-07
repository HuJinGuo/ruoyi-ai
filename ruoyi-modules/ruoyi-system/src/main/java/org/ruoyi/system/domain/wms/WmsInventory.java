package org.ruoyi.system.domain.wms;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import java.math.BigDecimal;

/**
 * XTP 库存表 wms_inventory
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_inventory")
public class WmsInventory extends TenantEntity {

    @TableId(value = "inventory_id")
    private Long inventoryId;

    private Long partId;

    private String partCode;

    private String partName;

    private String specification;

    private String unit;

    private BigDecimal stockQty;

    private BigDecimal availableQty;

    private BigDecimal lockedQty;

    private String locationCode;

    private String remark;

}
