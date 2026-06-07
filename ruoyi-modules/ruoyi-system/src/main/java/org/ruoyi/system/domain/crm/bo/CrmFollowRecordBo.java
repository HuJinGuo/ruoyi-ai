package org.ruoyi.system.domain.crm.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.crm.CrmFollowRecord;

import java.util.Date;

/**
 * CRM 跟进记录业务对象 crm_follow_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CrmFollowRecord.class, reverseConvertGenerate = false)
public class CrmFollowRecordBo extends BaseEntity {

    private Long followId;

    private Long opportunityId;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    private Long contactId;

    @NotNull(message = "跟进时间不能为空")
    private Date followTime;

    private String followMethod;

    @NotBlank(message = "跟进内容不能为空")
    private String content;

    private String result;

    private Date nextFollowTime;

    private String remark;
}
