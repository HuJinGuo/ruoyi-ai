package org.ruoyi.system.domain.crm.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import org.ruoyi.system.domain.crm.CrmPaymentPlan;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 回款计划视图对象 crm_payment_plan
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = CrmPaymentPlan.class)
public class CrmPaymentPlanVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "回款计划ID")
    private Long paymentId;

    @ExcelProperty(value = "合同ID")
    private Long contractId;

    @ExcelProperty(value = "合同名称")
    private String contractName;

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

    @ExcelProperty(value = "付款节点")
    private String stageName;

    @ExcelProperty(value = "金额")
    private BigDecimal amount;

    @ExcelProperty(value = "计划收款日期")
    private Date plannedDate;

    @ExcelProperty(value = "回款状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "crm_payment_status")
    private String status;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    @ExcelProperty(value = "备注")
    private String remark;
}
