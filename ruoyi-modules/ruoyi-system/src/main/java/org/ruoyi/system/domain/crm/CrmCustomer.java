package org.ruoyi.system.domain.crm;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

/**
 * CRM 客户表 crm_customer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_customer")
public class CrmCustomer extends TenantEntity {

    @TableId(value = "customer_id")
    private Long customerId;

    private String name;

    private String shortName;

    private String code;

    private String type;

    private String level;

    private String industry;

    private String province;

    private String city;

    private String district;

    private String address;

    private String website;

    private String status;

    private String scale;

    private String remark;
}
