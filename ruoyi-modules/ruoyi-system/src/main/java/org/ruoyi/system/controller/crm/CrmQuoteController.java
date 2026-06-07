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
import org.ruoyi.system.domain.crm.bo.CrmQuoteBo;
import org.ruoyi.system.domain.crm.vo.CrmQuoteVo;
import org.ruoyi.system.service.crm.CrmQuoteService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * CRM 报价管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crm/quote")
public class CrmQuoteController extends BaseController {

    private final CrmQuoteService quoteService;

    @SaCheckPermission("crm:quote:list")
    @GetMapping("/list")
    public TableDataInfo<CrmQuoteVo> list(CrmQuoteBo quote, PageQuery pageQuery) {
        return quoteService.selectPageList(quote, pageQuery);
    }

    @SaCheckPermission("crm:quote:query")
    @GetMapping("/options")
    public R<List<CrmQuoteVo>> options(CrmQuoteBo quote) {
        return R.ok(quoteService.selectList(quote));
    }

    @Log(title = "CRM报价", businessType = BusinessType.EXPORT)
    @SaCheckPermission("crm:quote:export")
    @PostMapping("/export")
    public void export(CrmQuoteBo quote, HttpServletResponse response) {
        ExcelUtil.exportExcel(quoteService.selectList(quote), "CRM报价", CrmQuoteVo.class, response);
    }

    @SaCheckPermission("crm:quote:query")
    @GetMapping("/{quoteId}")
    public R<CrmQuoteVo> getInfo(@PathVariable Long quoteId) {
        return R.ok(quoteService.selectById(quoteId));
    }

    @SaCheckPermission("crm:quote:add")
    @Log(title = "CRM报价", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody CrmQuoteBo quote) {
        return toAjax(quoteService.insert(quote));
    }

    @SaCheckPermission("crm:quote:edit")
    @Log(title = "CRM报价", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody CrmQuoteBo quote) {
        return toAjax(quoteService.update(quote));
    }

    @SaCheckPermission("crm:quote:remove")
    @Log(title = "CRM报价", businessType = BusinessType.DELETE)
    @DeleteMapping("/{quoteIds}")
    public R<Void> remove(@PathVariable Long[] quoteIds) {
        return toAjax(quoteService.deleteByIds(Arrays.asList(quoteIds)));
    }
}
