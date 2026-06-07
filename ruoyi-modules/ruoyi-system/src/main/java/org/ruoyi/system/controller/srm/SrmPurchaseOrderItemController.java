package org.ruoyi.system.controller.srm;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.srm.SrmPurchaseOrderItem;
import org.ruoyi.system.service.srm.SrmPurchaseOrderItemService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/srm/purchase-order-item")
public class SrmPurchaseOrderItemController extends XtpCrudController<SrmPurchaseOrderItem> {

    private final SrmPurchaseOrderItemService service;

    @Override
    protected XtpCrudService<SrmPurchaseOrderItem> service() {
        return service;
    }

    @Override
    protected String title() {
        return "采购订单明细";
    }

    @Override
    protected Class<SrmPurchaseOrderItem> entityClass() {
        return SrmPurchaseOrderItem.class;
    }
}
