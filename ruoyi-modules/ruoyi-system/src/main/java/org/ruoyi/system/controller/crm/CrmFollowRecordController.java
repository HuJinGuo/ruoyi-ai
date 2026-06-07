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
import org.ruoyi.system.domain.crm.bo.CrmFollowRecordBo;
import org.ruoyi.system.domain.crm.vo.CrmFollowRecordVo;
import org.ruoyi.system.service.crm.CrmFollowRecordService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * CRM 跟进记录管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crm/follow-record")
public class CrmFollowRecordController extends BaseController {

    private final CrmFollowRecordService followRecordService;

    @SaCheckPermission("crm:followRecord:list")
    @GetMapping("/list")
    public TableDataInfo<CrmFollowRecordVo> list(CrmFollowRecordBo followRecord, PageQuery pageQuery) {
        return followRecordService.selectPageList(followRecord, pageQuery);
    }

    @SaCheckPermission("crm:followRecord:query")
    @GetMapping("/options")
    public R<List<CrmFollowRecordVo>> options(CrmFollowRecordBo followRecord) {
        return R.ok(followRecordService.selectList(followRecord));
    }

    @Log(title = "CRM跟进记录", businessType = BusinessType.EXPORT)
    @SaCheckPermission("crm:followRecord:export")
    @PostMapping("/export")
    public void export(CrmFollowRecordBo followRecord, HttpServletResponse response) {
        ExcelUtil.exportExcel(followRecordService.selectList(followRecord), "CRM跟进记录", CrmFollowRecordVo.class, response);
    }

    @SaCheckPermission("crm:followRecord:query")
    @GetMapping("/{followId}")
    public R<CrmFollowRecordVo> getInfo(@PathVariable Long followId) {
        return R.ok(followRecordService.selectById(followId));
    }

    @SaCheckPermission("crm:followRecord:add")
    @Log(title = "CRM跟进记录", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody CrmFollowRecordBo followRecord) {
        return toAjax(followRecordService.insert(followRecord));
    }

    @SaCheckPermission("crm:followRecord:edit")
    @Log(title = "CRM跟进记录", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody CrmFollowRecordBo followRecord) {
        return toAjax(followRecordService.update(followRecord));
    }

    @SaCheckPermission("crm:followRecord:remove")
    @Log(title = "CRM跟进记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{followIds}")
    public R<Void> remove(@PathVariable Long[] followIds) {
        return toAjax(followRecordService.deleteByIds(Arrays.asList(followIds)));
    }
}
