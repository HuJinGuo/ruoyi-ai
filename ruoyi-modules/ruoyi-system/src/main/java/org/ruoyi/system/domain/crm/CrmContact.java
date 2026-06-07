package org.ruoyi.system.domain.crm;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

/**
 * CRM 联系人表 crm_contact
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_contact")
public class CrmContact extends TenantEntity {

    @TableId(value = "contact_id")
    private Long contactId;

    private Long customerId;

    private String name;

    private String phone;

    private String email;

    private String wechat;

    private String position;

    private String department;

    private String decisionRole;

    private String remark;
}
