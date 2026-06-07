package org.ruoyi.system.domain.crm.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import org.ruoyi.system.domain.crm.CrmContact;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * CRM 联系人视图对象 crm_contact
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = CrmContact.class)
public class CrmContactVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "联系人ID")
    private Long contactId;

    @ExcelProperty(value = "客户ID")
    private Long customerId;

    @ExcelProperty(value = "联系人姓名")
    private String name;

    @ExcelProperty(value = "手机")
    private String phone;

    @ExcelProperty(value = "邮箱")
    private String email;

    @ExcelProperty(value = "微信")
    private String wechat;

    @ExcelProperty(value = "职位")
    private String position;

    @ExcelProperty(value = "部门")
    private String department;

    @ExcelProperty(value = "决策角色", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "crm_decision_role")
    private String decisionRole;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    @ExcelProperty(value = "备注")
    private String remark;
}
