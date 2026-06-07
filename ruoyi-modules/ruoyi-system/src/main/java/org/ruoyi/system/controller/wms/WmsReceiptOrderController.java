package org.ruoyi.system.controller.wms;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.wms.WmsReceiptOrder;
import org.ruoyi.system.service.wms.WmsReceiptOrderService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wms/receipt-order")
public class WmsReceiptOrderController extends XtpCrudController<WmsReceiptOrder> {

    private final WmsReceiptOrderService service;

    @Override
    protected XtpCrudService<WmsReceiptOrder> service() {
        return service;
    }

    @Override
    protected String title() {
        return "收料单";
    }

    @Override
    protected Class<WmsReceiptOrder> entityClass() {
        return WmsReceiptOrder.class;
    }
}
