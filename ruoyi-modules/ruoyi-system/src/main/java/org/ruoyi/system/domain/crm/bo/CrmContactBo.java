package org.ruoyi.system.domain.crm.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.crm.CrmContact;

/**
 * CRM 联系人业务对象 crm_contact
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CrmContact.class, reverseConvertGenerate = false)
public class CrmContactBo extends BaseEntity {

    private Long contactId;

    @NotNull(message = "所属客户不能为空")
    private Long customerId;

    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 64, message = "联系人姓名长度不能超过{max}个字符")
    private String name;

    private String phone;

    private String email;

    private String wechat;

    private String position;

    private String department;

    private String decisionRole;

    private String remark;
}
