package org.ruoyi.system.domain.crm.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import org.ruoyi.system.domain.crm.CrmFollowRecord;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * CRM 跟进记录视图对象 crm_follow_record
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = CrmFollowRecord.class)
public class CrmFollowRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "跟进ID")
    private Long followId;

    @ExcelProperty(value = "商机ID")
    private Long opportunityId;

    @ExcelProperty(value = "商机名称")
    private String opportunityName;

    @ExcelProperty(value = "客户ID")
    private Long customerId;

    @ExcelProperty(value = "客户编码")
    private String customerCode;

    @ExcelProperty(value = "客户名称")
    private String customerName;

    @ExcelProperty(value = "联系人ID")
    private Long contactId;

    @ExcelProperty(value = "联系人")
    private String contactName;

    @ExcelProperty(value = "跟进时间")
    private Date followTime;

    @ExcelProperty(value = "跟进方式", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "crm_follow_method")
    private String followMethod;

    @ExcelProperty(value = "跟进内容")
    private String content;

    @ExcelProperty(value = "跟进结果", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "crm_follow_result")
    private String result;

    @ExcelProperty(value = "下次跟进时间")
    private Date nextFollowTime;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    @ExcelProperty(value = "备注")
    private String remark;
}
