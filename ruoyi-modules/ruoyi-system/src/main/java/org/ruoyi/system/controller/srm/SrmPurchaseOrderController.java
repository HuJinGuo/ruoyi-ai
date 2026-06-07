package org.ruoyi.system.controller.srm;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.srm.SrmPurchaseOrder;
import org.ruoyi.system.service.srm.SrmPurchaseOrderService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/srm/purchase-order")
public class SrmPurchaseOrderController extends XtpCrudController<SrmPurchaseOrder> {

    private final SrmPurchaseOrderService service;

    @Override
    protected XtpCrudService<SrmPurchaseOrder> service() {
        return service;
    }

    @Override
    protected String title() {
        return "采购订单";
    }

    @Override
    protected Class<SrmPurchaseOrder> entityClass() {
        return SrmPurchaseOrder.class;
    }
}
