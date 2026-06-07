package org.ruoyi.system.controller.crm;

import cn.dev33.satoken.annotation.SaCheckPermission;
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
import org.ruoyi.system.domain.crm.bo.CrmPaymentPlanBo;
import org.ruoyi.system.domain.crm.vo.CrmPaymentPlanVo;
import org.ruoyi.system.service.crm.CrmPaymentPlanService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * CRM 回款计划管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crm/payment-plan")
public class CrmPaymentPlanController extends BaseController {

    private final CrmPaymentPlanService paymentPlanService;

    @SaCheckPermission("crm:paymentPlan:list")
    @GetMapping("/list")
    public TableDataInfo<CrmPaymentPlanVo> list(CrmPaymentPlanBo paymentPlan, PageQuery pageQuery) {
        return paymentPlanService.selectPageList(paymentPlan, pageQuery);
    }

    @SaCheckPermission("crm:paymentPlan:query")
    @GetMapping("/options")
    public R<List<CrmPaymentPlanVo>> options(CrmPaymentPlanBo paymentPlan) {
        return R.ok(paymentPlanService.selectList(paymentPlan));
    }

    @Log(title = "CRM回款计划", businessType = BusinessType.EXPORT)
    @SaCheckPermission("crm:paymentPlan:export")
    @PostMapping("/export")
    public void export(CrmPaymentPlanBo paymentPlan, HttpServletResponse response) {
        ExcelUtil.exportExcel(paymentPlanService.selectList(paymentPlan), "CRM回款计划", CrmPaymentPlanVo.class, response);
    }

    @SaCheckPermission("crm:paymentPlan:query")
    @GetMapping("/{paymentId}")
    public R<CrmPaymentPlanVo> getInfo(@PathVariable Long paymentId) {
        return R.ok(paymentPlanService.selectById(paymentId));
    }

    @SaCheckPermission("crm:paymentPlan:add")
    @Log(title = "CRM回款计划", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody CrmPaymentPlanBo paymentPlan) {
        return toAjax(paymentPlanService.insert(paymentPlan));
    }

    @SaCheckPermission("crm:paymentPlan:edit")
    @Log(title = "CRM回款计划", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody CrmPaymentPlanBo paymentPlan) {
        return toAjax(paymentPlanService.update(paymentPlan));
    }

    @SaCheckPermission("crm:paymentPlan:remove")
    @Log(title = "CRM回款计划", businessType = BusinessType.DELETE)
    @DeleteMapping("/{paymentIds}")
    public R<Void> remove(@PathVariable Long[] paymentIds) {
        return toAjax(paymentPlanService.deleteByIds(Arrays.asList(paymentIds)));
    }
}
