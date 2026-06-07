package org.ruoyi.system.domain.crm.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import org.ruoyi.system.domain.crm.CrmContract;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 合同视图对象 crm_contract
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = CrmContract.class)
public class CrmContractVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "合同ID")
    private Long contractId;

    @ExcelProperty(value = "商机ID")
    private Long opportunityId;

    @ExcelProperty(value = "客户ID")
    private Long customerId;

    @ExcelProperty(value = "报价ID")
    private Long quoteId;

    @ExcelProperty(value = "合同名称")
    private String name;

    @ExcelProperty(value = "合同金额")
    private BigDecimal amount;

    @ExcelProperty(value = "签订日期")
    private Date signedDate;

    @ExcelProperty(value = "交付日期")
    private Date deliveryDate;

    @ExcelProperty(value = "合同状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "crm_contract_status")
    private String status;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    @ExcelProperty(value = "备注")
    private String remark;
}
