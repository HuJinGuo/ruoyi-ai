package org.ruoyi.system.controller.wms;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.wms.WmsReceiptOrderItem;
import org.ruoyi.system.service.wms.WmsReceiptOrderItemService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wms/receipt-order-item")
public class WmsReceiptOrderItemController extends XtpCrudController<WmsReceiptOrderItem> {

    private final WmsReceiptOrderItemService service;

    @Override
    protected XtpCrudService<WmsReceiptOrderItem> service() {
        return service;
    }

    @Override
    protected String title() {
        return "收料单明细";
    }

    @Override
    protected Class<WmsReceiptOrderItem> entityClass() {
        return WmsReceiptOrderItem.class;
    }
}
