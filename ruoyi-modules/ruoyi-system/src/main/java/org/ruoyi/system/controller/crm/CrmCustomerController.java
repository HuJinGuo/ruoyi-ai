package org.ruoyi.system.controller.crm;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.crm.bo.CrmCustomerBo;
import org.ruoyi.system.domain.crm.vo.CrmCustomerVo;
import org.ruoyi.system.service.crm.CrmCustomerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * CRM 客户管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crm/customer")
public class CrmCustomerController extends BaseController {

    private final CrmCustomerService customerService;

    @SaCheckPermission("crm:customer:list")
    @GetMapping("/list")
    public TableDataInfo<CrmCustomerVo> list(CrmCustomerBo customer, PageQuery pageQuery) {
        return customerService.selectPageList(customer, pageQuery);
    }

    @SaCheckPermission(value = {
        "crm:customer:query",
        "crm:contract:list",
        "crm:contract:query",
        "crm:contract:add",
        "crm:contract:edit"
    }, mode = SaMode.OR)
    @GetMapping("/options")
    public R<List<CrmCustomerVo>> options(CrmCustomerBo customer) {
        return R.ok(customerService.selectList(customer));
    }

    @Log(title = "CRM客户", businessType = BusinessType.EXPORT)
    @SaCheckPermission("crm:customer:export")
    @PostMapping("/export")
    public void export(CrmCustomerBo customer, HttpServletResponse response) {
        ExcelUtil.exportExcel(customerService.selectList(customer), "CRM客户", CrmCustomerVo.class, response);
    }

    @SaCheckPermission("crm:customer:query")
    @GetMapping("/{customerId}")
    public R<CrmCustomerVo> getInfo(@PathVariable Long customerId) {
        return R.ok(customerService.selectById(customerId));
    }

    @SaCheckPermission("crm:customer:add")
    @Log(title = "CRM客户", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody CrmCustomerBo customer) {
        if (!customerService.checkCodeUnique(customer)) {
            return R.fail("新增客户'" + customer.getName() + "'失败，客户编码已存在");
        }
        return toAjax(customerService.insert(customer));
    }

    @SaCheckPermission("crm:customer:edit")
    @Log(title = "CRM客户", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody CrmCustomerBo customer) {
        if (!customerService.checkCodeUnique(customer)) {
            return R.fail("修改客户'" + customer.getName() + "'失败，客户编码已存在");
        }
        return toAjax(customerService.update(customer));
    }

    @SaCheckPermission("crm:customer:remove")
    @Log(title = "CRM客户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{customerIds}")
    public R<Void> remove(@PathVariable Long[] customerIds) {
        return toAjax(customerService.deleteByIds(Arrays.asList(customerIds)));
    }
}
