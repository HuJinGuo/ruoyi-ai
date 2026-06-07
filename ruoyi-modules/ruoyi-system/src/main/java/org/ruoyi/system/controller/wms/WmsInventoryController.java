package org.ruoyi.system.controller.wms;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.wms.WmsInventory;
import org.ruoyi.system.service.wms.WmsInventoryService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wms/inventory")
public class WmsInventoryController extends XtpCrudController<WmsInventory> {

    private final WmsInventoryService service;

    @Override
    protected XtpCrudService<WmsInventory> service() {
        return service;
    }

    @Override
    protected String title() {
        return "库存";
    }

    @Override
    protected Class<WmsInventory> entityClass() {
        return WmsInventory.class;
    }
}
