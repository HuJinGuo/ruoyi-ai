package org.ruoyi.system.domain.crm.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.crm.CrmCustomer;

/**
 * CRM 客户业务对象 crm_customer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CrmCustomer.class, reverseConvertGenerate = false)
public class CrmCustomerBo extends BaseEntity {

    private Long customerId;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 120, message = "客户名称长度不能超过{max}个字符")
    private String name;

    @Size(max = 80, message = "客户简称长度不能超过{max}个字符")
    private String shortName;

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 64, message = "客户编码长度不能超过{max}个字符")
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
