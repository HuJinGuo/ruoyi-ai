package org.ruoyi.system.controller.wms;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.wms.WmsIssueOrder;
import org.ruoyi.system.service.wms.WmsIssueOrderService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wms/issue-order")
public class WmsIssueOrderController extends XtpCrudController<WmsIssueOrder> {

    private final WmsIssueOrderService service;

    @Override
    protected XtpCrudService<WmsIssueOrder> service() {
        return service;
    }

    @Override
    protected String title() {
        return "发料单";
    }

    @Override
    protected Class<WmsIssueOrder> entityClass() {
        return WmsIssueOrder.class;
    }
}
