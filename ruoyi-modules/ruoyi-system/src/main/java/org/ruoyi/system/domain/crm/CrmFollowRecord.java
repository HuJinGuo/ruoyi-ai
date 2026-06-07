package org.ruoyi.system.domain.crm;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.util.Date;

/**
 * CRM 跟进记录表 crm_follow_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_follow_record")
public class CrmFollowRecord extends TenantEntity {

    @TableId(value = "follow_id")
    private Long followId;

    private Long opportunityId;

    private Long customerId;

    private Long contactId;

    private Date followTime;

    private String followMethod;

    private String content;

    private String result;

    private Date nextFollowTime;

    private String remark;
}
