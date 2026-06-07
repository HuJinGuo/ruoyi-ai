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
import org.ruoyi.system.domain.crm.bo.CrmContractBo;
import org.ruoyi.system.domain.crm.vo.CrmContractVo;
import org.ruoyi.system.service.crm.CrmContractService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * CRM 合同管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crm/contract")
public class CrmContractController extends BaseController {

    private final CrmContractService contractService;

    @SaCheckPermission("crm:contract:list")
    @GetMapping("/list")
    public TableDataInfo<CrmContractVo> list(CrmContractBo contract, PageQuery pageQuery) {
        return contractService.selectPageList(contract, pageQuery);
    }

    @SaCheckPermission("crm:contract:query")
    @GetMapping("/options")
    public R<List<CrmContractVo>> options(CrmContractBo contract) {
        return R.ok(contractService.selectList(contract));
    }

    @Log(title = "CRM合同", businessType = BusinessType.EXPORT)
    @SaCheckPermission("crm:contract:export")
    @PostMapping("/export")
    public void export(CrmContractBo contract, HttpServletResponse response) {
        ExcelUtil.exportExcel(contractService.selectList(contract), "CRM合同", CrmContractVo.class, response);
    }

    @SaCheckPermission("crm:contract:query")
    @GetMapping("/{contractId}")
    public R<CrmContractVo> getInfo(@PathVariable Long contractId) {
        return R.ok(contractService.selectById(contractId));
    }

    @SaCheckPermission("crm:contract:add")
    @Log(title = "CRM合同", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody CrmContractBo contract) {
        return toAjax(contractService.insert(contract));
    }

    @SaCheckPermission("crm:contract:edit")
    @Log(title = "CRM合同", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody CrmContractBo contract) {
        return toAjax(contractService.update(contract));
    }

    @SaCheckPermission("crm:contract:remove")
    @Log(title = "CRM合同", businessType = BusinessType.DELETE)
    @DeleteMapping("/{contractIds}")
    public R<Void> remove(@PathVariable Long[] contractIds) {
        return toAjax(contractService.deleteByIds(Arrays.asList(contractIds)));
    }
}
