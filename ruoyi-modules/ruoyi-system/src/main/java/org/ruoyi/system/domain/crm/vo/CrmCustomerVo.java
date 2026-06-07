package org.ruoyi.system.domain.crm.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import org.ruoyi.system.domain.crm.CrmCustomer;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * CRM 客户视图对象 crm_customer
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = CrmCustomer.class)
public class CrmCustomerVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "客户ID")
    private Long customerId;

    @ExcelProperty(value = "客户名称")
    private String name;

    @ExcelProperty(value = "客户简称")
    private String shortName;

    @ExcelProperty(value = "客户编码")
    private String code;

    @ExcelProperty(value = "客户类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "crm_customer_type")
    private String type;

    @ExcelProperty(value = "客户等级", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "crm_customer_level")
    private String level;

    @ExcelProperty(value = "行业")
    private String industry;

    @ExcelProperty(value = "省")
    private String province;

    @ExcelProperty(value = "市")
    private String city;

    @ExcelProperty(value = "区")
    private String district;

    @ExcelProperty(value = "详细地址")
    private String address;

    @ExcelProperty(value = "官网")
    private String website;

    @ExcelProperty(value = "客户状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "crm_customer_status")
    private String status;

    @ExcelProperty(value = "规模信息")
    private String scale;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    @ExcelProperty(value = "备注")
    private String remark;
}
