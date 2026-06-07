package org.ruoyi.system.controller.xtp;

import jakarta.servlet.http.HttpServletResponse;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.tenant.core.TenantEntity;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

public abstract class XtpCrudController<T extends TenantEntity> extends BaseController {

    protected abstract XtpCrudService<T> service();

    protected abstract String title();

    @GetMapping("/list")
    public TableDataInfo<T> list(T query, PageQuery pageQuery) {
        return service().selectPageList(query, pageQuery);
    }

    @GetMapping("/options")
    public R<List<T>> options(T query) {
        return R.ok(service().selectList(query));
    }

    @Log(title = "XTP数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(T query, HttpServletResponse response) {
        ExcelUtil.exportExcel(service().selectList(query), title(), entityClass(), response);
    }

    @GetMapping("/{id}")
    public R<T> getInfo(@PathVariable Long id) {
        return R.ok(service().selectById(id));
    }

    @Log(title = "XTP数据", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@RequestBody T entity) {
        return toAjax(service().insert(entity));
    }

    @Log(title = "XTP数据", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@RequestBody T entity) {
        return toAjax(service().update(entity));
    }

    @Log(title = "XTP数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(service().deleteByIds(Arrays.asList(ids)));
    }

    protected abstract Class<T> entityClass();
}
