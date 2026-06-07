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
import org.ruoyi.system.domain.crm.bo.CrmOpportunityBo;
import org.ruoyi.system.domain.crm.vo.CrmOpportunityVo;
import org.ruoyi.system.service.crm.CrmOpportunityService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * CRM 商机管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crm/opportunity")
public class CrmOpportunityController extends BaseController {

    private final CrmOpportunityService opportunityService;

    @SaCheckPermission("crm:opportunity:list")
    @GetMapping("/list")
    public TableDataInfo<CrmOpportunityVo> list(CrmOpportunityBo opportunity, PageQuery pageQuery) {
        return opportunityService.selectPageList(opportunity, pageQuery);
    }

    @SaCheckPermission("crm:opportunity:query")
    @GetMapping("/options")
    public R<List<CrmOpportunityVo>> options(CrmOpportunityBo opportunity) {
        return R.ok(opportunityService.selectList(opportunity));
    }

    @Log(title = "CRM商机", businessType = BusinessType.EXPORT)
    @SaCheckPermission("crm:opportunity:export")
    @PostMapping("/export")
    public void export(CrmOpportunityBo opportunity, HttpServletResponse response) {
        ExcelUtil.exportExcel(opportunityService.selectList(opportunity), "CRM商机", CrmOpportunityVo.class, response);
    }

    @SaCheckPermission("crm:opportunity:query")
    @GetMapping("/{opportunityId}")
    public R<CrmOpportunityVo> getInfo(@PathVariable Long opportunityId) {
        return R.ok(opportunityService.selectById(opportunityId));
    }

    @SaCheckPermission("crm:opportunity:add")
    @Log(title = "CRM商机", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody CrmOpportunityBo opportunity) {
        return toAjax(opportunityService.insert(opportunity));
    }

    @SaCheckPermission("crm:opportunity:edit")
    @Log(title = "CRM商机", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody CrmOpportunityBo opportunity) {
        return toAjax(opportunityService.update(opportunity));
    }

    @SaCheckPermission("crm:opportunity:remove")
    @Log(title = "CRM商机", businessType = BusinessType.DELETE)
    @DeleteMapping("/{opportunityIds}")
    public R<Void> remove(@PathVariable Long[] opportunityIds) {
        return toAjax(opportunityService.deleteByIds(Arrays.asList(opportunityIds)));
    }
}
