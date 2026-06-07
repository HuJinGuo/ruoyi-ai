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
import org.ruoyi.system.domain.crm.bo.CrmContactBo;
import org.ruoyi.system.domain.crm.vo.CrmContactVo;
import org.ruoyi.system.service.crm.CrmContactService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * CRM 联系人管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crm/contact")
public class CrmContactController extends BaseController {

    private final CrmContactService contactService;

    @SaCheckPermission("crm:contact:list")
    @GetMapping("/list")
    public TableDataInfo<CrmContactVo> list(CrmContactBo contact, PageQuery pageQuery) {
        return contactService.selectPageList(contact, pageQuery);
    }

    @SaCheckPermission("crm:contact:query")
    @GetMapping("/options")
    public R<List<CrmContactVo>> options(CrmContactBo contact) {
        return R.ok(contactService.selectList(contact));
    }

    @Log(title = "CRM联系人", businessType = BusinessType.EXPORT)
    @SaCheckPermission("crm:contact:export")
    @PostMapping("/export")
    public void export(CrmContactBo contact, HttpServletResponse response) {
        ExcelUtil.exportExcel(contactService.selectList(contact), "CRM联系人", CrmContactVo.class, response);
    }

    @SaCheckPermission("crm:contact:query")
    @GetMapping("/{contactId}")
    public R<CrmContactVo> getInfo(@PathVariable Long contactId) {
        return R.ok(contactService.selectById(contactId));
    }

    @SaCheckPermission("crm:contact:add")
    @Log(title = "CRM联系人", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody CrmContactBo contact) {
        return toAjax(contactService.insert(contact));
    }

    @SaCheckPermission("crm:contact:edit")
    @Log(title = "CRM联系人", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody CrmContactBo contact) {
        return toAjax(contactService.update(contact));
    }

    @SaCheckPermission("crm:contact:remove")
    @Log(title = "CRM联系人", businessType = BusinessType.DELETE)
    @DeleteMapping("/{contactIds}")
    public R<Void> remove(@PathVariable Long[] contactIds) {
        return toAjax(contactService.deleteByIds(Arrays.asList(contactIds)));
    }
}
