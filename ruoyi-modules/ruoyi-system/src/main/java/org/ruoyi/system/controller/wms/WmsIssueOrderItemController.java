package org.ruoyi.system.controller.wms;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.wms.WmsIssueOrderItem;
import org.ruoyi.system.service.wms.WmsIssueOrderItemService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wms/issue-order-item")
public class WmsIssueOrderItemController extends XtpCrudController<WmsIssueOrderItem> {

    private final WmsIssueOrderItemService service;

    @Override
    protected XtpCrudService<WmsIssueOrderItem> service() {
        return service;
    }

    @Override
    protected String title() {
        return "发料单明细";
    }

    @Override
    protected Class<WmsIssueOrderItem> entityClass() {
        return WmsIssueOrderItem.class;
    }
}
