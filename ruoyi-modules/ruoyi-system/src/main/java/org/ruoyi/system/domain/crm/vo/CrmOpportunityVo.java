package org.ruoyi.system.domain.crm.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import org.ruoyi.system.domain.crm.CrmOpportunity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 商机视图对象 crm_opportunity
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = CrmOpportunity.class)
public class CrmOpportunityVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "商机ID")
    private Long opportunityId;

    @ExcelProperty(value = "客户ID")
    private Long customerId;

    @ExcelProperty(value = "联系人ID")
    private Long contactId;

    @ExcelProperty(value = "商机名称")
    private String name;

    @ExcelProperty(value = "预计金额")
    private BigDecimal estimatedAmount;

    @ExcelProperty(value = "预计签单日期")
    private Date estimatedCloseDate;

    @ExcelProperty(value = "项目来源", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "crm_opportunity_source")
    private String source;

    @ExcelProperty(value = "销售阶段", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "crm_opportunity_stage")
    private String stage;

    @ExcelProperty(value = "成功率")
    private BigDecimal successRate;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    @ExcelProperty(value = "备注")
    private String remark;
}
