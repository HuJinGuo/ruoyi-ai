package org.ruoyi.system.domain.crm.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import org.ruoyi.system.domain.crm.CrmQuote;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 报价视图对象 crm_quote
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = CrmQuote.class)
public class CrmQuoteVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "报价ID")
    private Long quoteId;

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

    @ExcelProperty(value = "版本")
    private Integer version;

    @ExcelProperty(value = "总金额")
    private BigDecimal totalAmount;

    @ExcelProperty(value = "报价状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "crm_quote_status")
    private String status;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    @ExcelProperty(value = "备注")
    private String remark;
}
