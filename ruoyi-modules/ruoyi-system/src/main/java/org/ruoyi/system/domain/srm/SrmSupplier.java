package org.ruoyi.system.domain.srm;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

/**
 * XTP 供应商表 srm_supplier
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("srm_supplier")
public class SrmSupplier extends TenantEntity {

    @TableId(value = "supplier_id")
    private Long supplierId;

    private String supplierCode;

    private String supplierName;

    private String shortName;

    private String contactName;

    private String phone;

    private String email;

    private String address;

    private String level;

    private String status;

    private String remark;

}
